package com.ams.similarproducts.infrastructure.controller;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.domain.usecase.GetSimilarProductsUseCase;
import com.ams.similarproducts.infrastructure.controller.dto.ProductResponseDto;
import com.ams.similarproducts.infrastructure.controller.mapper.ProductResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class SimilarProductsController {

    private final GetSimilarProductsUseCase getSimilarProductsUseCase;
    private final ProductResponseMapper productResponseMapper;

    @GetMapping("/{productId}/similar")
    public Mono<ResponseEntity<List<ProductResponseDto>>> getSimilarProducts(@PathVariable String productId) {
        final Mono<List<Product>> products = getSimilarProductsUseCase.getSimilarProducts(productId);

        return products.map(productResponseMapper::toProductResponseList).map(ResponseEntity::ok);
    }
}