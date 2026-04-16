package com.ams.similarproducts.domain.external;

import reactor.core.publisher.Mono;

import java.util.List;

public interface GetSimilarProductsPort {

    Mono<List<String>> getSimilarProductsIds(String productId);

}
