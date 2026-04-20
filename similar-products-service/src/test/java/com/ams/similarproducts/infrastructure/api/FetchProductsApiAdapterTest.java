package com.ams.similarproducts.infrastructure.api;

import com.ams.similarproducts.domain.config.ExternalProps;
import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.infrastructure.api.mapper.ProductApiMapper;
import com.ams.similarproducts.infrastructure.cache.ReactiveProductCache;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FetchProductsApiAdapterTest {

    @Mock
    private ProductApiMapper productApiMapper;

    @Mock
    private ExternalProps externalProps;

    @Mock
    private ReactiveProductCache reactiveProductCache;

    private CircuitBreaker circuitBreaker;

    private FetchProductsApiAdapter adapter;

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
        circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.ofDefaults());
        final WebClient webClient = WebClient.builder().baseUrl("http://localhost:3001/product").build();
        adapter = new FetchProductsApiAdapter(webClient, productApiMapper, externalProps, reactiveProductCache, circuitBreaker);
    }

    @Test
    void fetchProductById_shouldDelegateToCacheAndReturnProduct() {
        final String productId = "1";
        final Product product = buildProduct(productId);

        when(externalProps.getMaxTimeout()).thenReturn(5);
        when(reactiveProductCache.cache(anyString(), any()))
                .thenReturn(Mono.just(product));

        StepVerifier.create(adapter.fetchProductById(productId))
                .expectNext(product)
                .verifyComplete();

        verify(reactiveProductCache).cache(eq(productId), any());
    }

    @Test
    void fetchProductById_shouldPropagateEmptyWhenCacheReturnsEmpty() {
        final String productId = "1";

        when(externalProps.getMaxTimeout()).thenReturn(5);
        when(reactiveProductCache.cache(anyString(), any()))
                .thenReturn(Mono.empty());

        StepVerifier.create(adapter.fetchProductById(productId))
                .verifyComplete();
    }
}
