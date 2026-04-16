package com.ams.similarproducts.infrastructure.api;

import com.ams.similarproducts.domain.config.ExternalProps;
import com.ams.similarproducts.domain.exception.NotFoundException;
import com.ams.similarproducts.domain.external.GetSimilarProductsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetSimilarProductsApiAdapter implements GetSimilarProductsPort {

    @Qualifier("similarProductsWebClient")
    private final WebClient webClient;

    private final ExternalProps externalProps;

    @Override
    public Mono<List<String>> getSimilarProductsIds(String productId) {
        log.info("[GetSimilarProductsApiAdapter::getSimilarProducts] fetching similar products for product ID: {}",
                productId);

        return webClient.get()
                .uri("/{productId}/similarids", productId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<String>>() {})
                .timeout(Duration.ofSeconds(this.externalProps.getMaxTimeout()))
                .onErrorResume(WebClientResponseException.class, e ->
                        e.getStatusCode() == HttpStatus.NOT_FOUND
                                ? Mono.error(new NotFoundException("Product not found"))
                                : Mono.error(e)
                );
    }

}
