package com.ams.similarproducts.domain.usecase;

import com.ams.similarproducts.domain.entity.Product;
import reactor.core.publisher.Mono;

import java.util.List;

public interface GetSimilarProductsUseCase {

    Mono<List<Product>> getSimilarProducts(String productId);

}
