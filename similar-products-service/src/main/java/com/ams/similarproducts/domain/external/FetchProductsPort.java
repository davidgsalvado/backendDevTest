package com.ams.similarproducts.domain.external;

import com.ams.similarproducts.domain.entity.Product;
import reactor.core.publisher.Mono;

public interface FetchProductsPort {

    Mono<Product> fetchProductById(String productId);

}
