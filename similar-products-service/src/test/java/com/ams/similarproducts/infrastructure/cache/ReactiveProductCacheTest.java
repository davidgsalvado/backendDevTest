package com.ams.similarproducts.infrastructure.cache;

import com.ams.similarproducts.domain.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveProductCacheTest {

    private ReactiveProductCache cache;

    private static Product buildProduct(String id) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .price(BigDecimal.valueOf(10.00))
                .availability(true)
                .build();
    }

    @BeforeEach
    void setUp() {
        cache = new ReactiveProductCache();
    }

    @Test
    void cache_shouldCallUpstreamAndCacheResult() {
        final Product product = buildProduct("1");

        StepVerifier.create(cache.cache("1", Mono.just(product)))
                .expectNext(product)
                .verifyComplete();

        assertEquals(1, cache.size());
        assertTrue(cache.getCachedProductIds().contains("1"));
    }

    @Test
    void cache_shouldReturnCachedProductOnSecondCall() {
        final Product product = buildProduct("1");

        // First call — caches the product
        StepVerifier.create(cache.cache("1", Mono.just(product)))
                .expectNext(product)
                .verifyComplete();

        // Second call — should return from cache, upstream not needed
        StepVerifier.create(cache.cache("1", Mono.error(new RuntimeException("should not call upstream"))))
                .expectNext(product)
                .verifyComplete();
    }

    @Test
    void put_shouldStoreProductDirectly() {
        final Product product = buildProduct("1");
        cache.put("1", product);

        assertEquals(1, cache.size());
        assertTrue(cache.getCachedProductIds().contains("1"));

        // Cached product should be returned even with error upstream
        StepVerifier.create(cache.cache("1", Mono.error(new RuntimeException("should not call"))))
                .expectNext(product)
                .verifyComplete();
    }

    @Test
    void clear_shouldRemoveAllEntries() {
        cache.put("1", buildProduct("1"));
        cache.put("2", buildProduct("2"));

        assertEquals(2, cache.size());

        cache.clear();

        assertEquals(0, cache.size());
        assertTrue(cache.getCachedProductIds().isEmpty());
    }

    @Test
    void getStats_shouldReturnCacheStatistics() {
        cache.put("1", buildProduct("1"));

        final Map<String, Object> stats = cache.getStats();

        assertEquals(1, stats.get("size"));
        assertEquals(0, stats.get("inflightRequests"));
        assertTrue(((java.util.Set<?>) stats.get("cachedProductIds")).contains("1"));
    }

    @Test
    void cache_shouldHandleUpstreamError() {
        StepVerifier.create(cache.cache("1", Mono.error(new RuntimeException("upstream error"))))
                .verifyError(RuntimeException.class);
    }
}
