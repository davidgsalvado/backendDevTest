package com.ams.similarproducts.infrastructure.api;

import com.ams.similarproducts.domain.config.ExternalProps;
import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.domain.exception.NotFoundException;
import com.ams.similarproducts.domain.external.FetchProductsPort;
import com.ams.similarproducts.infrastructure.api.dto.ProductDetailDto;
import com.ams.similarproducts.infrastructure.api.mapper.ProductApiMapper;
import com.ams.similarproducts.infrastructure.cache.ReactiveProductCache;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class FetchProductsApiAdapter implements FetchProductsPort {

    @Qualifier("similarProductsWebClient")
    private final WebClient webClient;

    private final ProductApiMapper productApiMapper;

    private final ExternalProps externalProps;

    private final ReactiveProductCache reactiveProductCache;

    private final CircuitBreaker productCircuitBreaker;

    @Override
    public Mono<Product> fetchProductById(String productId) {
        final Mono<Product> apiCall = this.webClient.get()
                .uri("/{productId}", productId)
                .retrieve()
                .bodyToMono(ProductDetailDto.class)
                .timeout(Duration.ofSeconds(this.externalProps.getMaxTimeout()))
                .doOnNext(dto -> log.debug("[FetchProductsApiAdapter::fetchProductById] Product retrieved from API: {}", dto))
                .map(this.productApiMapper::toDomain)
                .doOnError(throwable -> log.error("[FetchProductsApiAdapter::fetchProductById] Error fetching product {}: {}", productId, throwable.getMessage()))
                .onErrorMap(WebClientResponseException.class, e ->
                        e.getStatusCode() == HttpStatus.NOT_FOUND
                                ? new NotFoundException("Product not found")
                                : e
                )
                .transformDeferred(CircuitBreakerOperator.of(productCircuitBreaker))
                .onErrorResume(CallNotPermittedException.class, e -> {
                    log.warn("[FetchProductsApiAdapter] Circuit OPEN for product {} - serving from cache if available", productId);
                    return Mono.empty();
                });

        return this.reactiveProductCache.cache(productId, apiCall);
    }
}
