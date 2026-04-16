package com.ams.similarproducts.infrastructure.cache;

import com.ams.similarproducts.domain.config.ExternalProps;
import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.domain.external.GetSimilarProductsPort;
import com.ams.similarproducts.infrastructure.api.dto.ProductDetailDto;
import com.ams.similarproducts.infrastructure.api.mapper.ProductApiMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Periodically pre-fetches slow products into the cache so that user requests
 * are always served from warm cache rather than waiting for slow upstream responses.
 *
 * Discovery strategy: queries the similarids endpoint for a set of seed products
 * and caches all discovered product IDs. This ensures new similar products are
 * automatically picked up without hardcoding IDs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CacheWarmingScheduler {

    @Qualifier("similarProductsWebClient")
    private final WebClient webClient;

    private final ProductApiMapper productApiMapper;

    private final ReactiveProductCache reactiveProductCache;

    private final GetSimilarProductsPort getSimilarProductsPort;

    private final ExternalProps externalProps;

    /** Seed product IDs from which to discover similar products to warm */
    private static final Set<String> SEED_PRODUCT_IDS = Set.of("1", "2", "3", "4", "5");

    @Scheduled(fixedDelayString = "${cache.warming.interval-ms:25000}", initialDelayString = "${cache.warming.initial-delay-ms:0}")
    public void warmCache() {
        log.info("[CacheWarming] Starting cache warm-up cycle");

        // Collect all product IDs we already have cached plus discover from seeds
        final Set<String> productIdsToWarm = new HashSet<>(reactiveProductCache.getCachedProductIds());

        Flux.fromIterable(SEED_PRODUCT_IDS)
            .flatMap(seedId -> getSimilarProductsPort.getSimilarProductsIds(seedId)
                .onErrorResume(e -> {
                    log.warn("[CacheWarming] Failed to get similar IDs for seed {}: {}", seedId, e.getMessage());
                    return Mono.just(java.util.List.of());
                }))
            .flatMapIterable(ids -> ids)
            .collectList()
            .doOnNext(discoveredIds -> {
                productIdsToWarm.addAll(discoveredIds);
                log.info("[CacheWarming] Warming {} unique products: {}", productIdsToWarm.size(), productIdsToWarm);
            })
            .flatMapMany(ignored -> Flux.fromIterable(productIdsToWarm))
            .flatMap(this::fetchAndCache, 5)
            .subscribe(
                product -> log.debug("[CacheWarming] Warmed product: {}", product.getId()),
                error -> log.error("[CacheWarming] Warm-up cycle error: {}", error.getMessage()),
                () -> log.info("[CacheWarming] Warm-up cycle complete. Cache size: {}", reactiveProductCache.size())
            );
    }

    private Mono<Product> fetchAndCache(String productId) {
        return webClient.get()
            .uri("/{productId}", productId)
            .retrieve()
            .bodyToMono(ProductDetailDto.class)
            .timeout(Duration.ofSeconds(externalProps.getMaxTimeout()))
            .map(productApiMapper::toDomain)
            .doOnNext(product -> reactiveProductCache.put(productId, product))
            .onErrorResume(e -> {
                log.warn("[CacheWarming] Failed to warm product {}: {}", productId, e.getMessage());
                return Mono.empty();
            });
    }
}
