package com.ams.similarproducts.infrastructure.cache;

import com.ams.similarproducts.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Reactive product cache implementing three complementary patterns:
 *
 * 1. Request Coalescing (Singleflight): concurrent requests for the same product
 *    share a single upstream call via Mono.cache().
 * 2. Stale-While-Revalidate: expired entries are returned immediately while a
 *    background refresh is triggered, so users never wait for slow upstream calls.
 * 3. Standard TTL caching: fresh entries are served directly without upstream calls.
 */
@Service
@Slf4j
public class ReactiveProductCache {

    private final Map<String, CachedProduct> cache = new ConcurrentHashMap<>();
    private final Map<String, Mono<Product>> inflightRequests = new ConcurrentHashMap<>();
    private static final long TTL_MILLIS = TimeUnit.SECONDS.toMillis(30);

    public Mono<Product> cache(String productId, Mono<Product> productMono) {
        final CachedProduct cached = cache.get(productId);

        if (cached != null) {
            if (!cached.isExpired()) {
                log.info("[ReactiveProductCache] CACHE HIT - Product ID: {} - Expires in: {} ms",
                    productId, cached.getTimeToLive());
                return Mono.just(cached.getProduct());
            }

            // Stale-While-Revalidate: return stale data immediately, refresh in background
            log.info("[ReactiveProductCache] STALE HIT - Product ID: {} - Refreshing in background", productId);
            triggerBackgroundRefresh(productId, productMono);
            return Mono.just(cached.getProduct());
        }

        // No cache entry: use request coalescing so concurrent callers share one upstream call
        return inflightRequests.computeIfAbsent(productId, key ->
            productMono
                .doOnNext(product -> {
                    cache.put(key, new CachedProduct(product));
                    log.info("[ReactiveProductCache] CACHE SET - Product ID: {} cached successfully", key);
                })
                .doOnError(throwable ->
                    log.error("[ReactiveProductCache] CACHE ERROR - Product ID: {} - Error: {}", key, throwable.getMessage())
                )
                .doFinally(signal -> inflightRequests.remove(key))
                .cache() // Reactor Mono.cache() converts cold→hot, sharing result with all subscribers
        );
    }

    private void triggerBackgroundRefresh(String productId, Mono<Product> productMono) {
        // Only trigger one background refresh per product at a time
        inflightRequests.computeIfAbsent(productId, key ->
            productMono
                .doOnNext(product -> {
                    cache.put(key, new CachedProduct(product));
                    log.info("[ReactiveProductCache] BACKGROUND REFRESH - Product ID: {} refreshed", key);
                })
                .doOnError(throwable ->
                    log.warn("[ReactiveProductCache] BACKGROUND REFRESH FAILED - Product ID: {} - Error: {}", key, throwable.getMessage())
                )
                .doFinally(signal -> inflightRequests.remove(key))
                .cache()
        ).subscribe(); // Fire-and-forget
    }

    public void put(String productId, Product product) {
        cache.put(productId, new CachedProduct(product));
        log.info("[ReactiveProductCache] CACHE PUT - Product ID: {} stored directly", productId);
    }

    public void clear() {
        log.info("[ReactiveProductCache] Clearing all cached products. Size before: {}", cache.size());
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public Set<String> getCachedProductIds() {
        return cache.keySet();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("size", cache.size());
        stats.put("inflightRequests", inflightRequests.size());
        stats.put("cachedProductIds", cache.keySet());
        return stats;
    }

    private static class CachedProduct {
        private final Product product;
        private final long expiresAt;

        CachedProduct(Product product) {
            this.product = product;
            this.expiresAt = System.currentTimeMillis() + TTL_MILLIS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }

        long getTimeToLive() {
            return Math.max(0, expiresAt - System.currentTimeMillis());
        }

        Product getProduct() {
            return product;
        }
    }
}

