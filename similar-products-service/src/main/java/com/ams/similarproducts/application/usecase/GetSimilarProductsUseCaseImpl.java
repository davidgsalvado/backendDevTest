package com.ams.similarproducts.application.usecase;

import com.ams.similarproducts.domain.external.FetchProductsPort;
import com.ams.similarproducts.domain.external.GetSimilarProductsPort;
import com.ams.similarproducts.domain.usecase.GetSimilarProductsUseCase;
import com.ams.similarproducts.domain.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetSimilarProductsUseCaseImpl implements GetSimilarProductsUseCase {

    private final FetchProductsPort fetchProductsPort;

    private final GetSimilarProductsPort getSimilarProductsPort;

    @Override
    public Mono<List<Product>> getSimilarProducts(String productId) {
        log.info("[GetSimilarProductsUseCaseImpl::getSimilarProducts] Fetching similar products for ID: {}", productId);
        final Mono<List<String>> similarIds = this.getSimilarProductsPort.getSimilarProductsIds(productId);

        return similarIds.flatMapMany(Flux::fromIterable)
                .distinct()
                .flatMap(this.fetchProductsPort::fetchProductById, 50)
                .onErrorContinue((throwable, o) -> log.warn("[GetSimilarProductsUseCaseImpl] Skipping product due to error: {}", throwable.getMessage()))
                .collectList();
    }
}