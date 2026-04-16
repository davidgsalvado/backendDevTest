package com.ams.similarproducts.infrastructure.cache;

import com.ams.similarproducts.domain.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Servicio de caché reactivo para productos.
 * Usa un enfoque nativo de Reactor para cachear Mono<Product>.
 */
@Service
@Slf4j
public class ReactiveProductCache {

    private final Map<String, CachedProduct> cache = new ConcurrentHashMap<>();
    private static final long TTL_MILLIS = TimeUnit.MINUTES.toMillis(10);

    public Mono<Product> cache(String productId, Mono<Product> productMono) {
        final CachedProduct cached = cache.get(productId);

        if (cached != null && !cached.isExpired()) {
            log.info("[ReactiveProductCache] CACHE HIT - Product ID: {} - Expires in: {} ms",
                productId, cached.getTimeToLive());
            return Mono.just(cached.getProduct());
        }

        return productMono
                .doOnNext(product -> {
                    cache.put(productId, new CachedProduct(product));
                    log.info("[ReactiveProductCache] CACHE SET - Product ID: {} cached successfully", productId);
                })
                .doOnError(throwable ->
                    log.error("[ReactiveProductCache] CACHE ERROR - Product ID: {} - Error: {}", productId, throwable.getMessage())
                );
    }

    public void clear() {
        log.info("[ReactiveProductCache] Clearing all cached products. Size before: {}", cache.size());
        cache.clear();
    }

    public int size() {
        return cache.size();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("size", cache.size());
        stats.put("hitRate", calculateHitRate());
        stats.put("cachedProductIds", cache.keySet());
        return stats;
    }

    private String calculateHitRate() {
        // Esta es una versión simplificada
        return String.format("Size: %d items", cache.size());
    }

    /**
     * Clase interna para almacenar productos con timestamp de expiración
     */
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

