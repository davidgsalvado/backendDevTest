package com.ams.similarproducts.infrastructure.controller;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.domain.usecase.GetSimilarProductsUseCase;
import com.ams.similarproducts.infrastructure.controller.dto.ProductResponseDto;
import com.ams.similarproducts.infrastructure.controller.mapper.ProductResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimilarProductsControllerTest {

    @Mock
    private GetSimilarProductsUseCase getSimilarProductsUseCase;

    @Mock
    private ProductResponseMapper productResponseMapper;

    @InjectMocks
    private SimilarProductsController controller;

    private static Product buildProduct(String id) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .price(BigDecimal.valueOf(10.00))
                .availability(true)
                .build();
    }

    private static ProductResponseDto buildProductResponseDto(String id) {
        return ProductResponseDto.builder()
                .id(id)
                .name("Product " + id)
                .price(BigDecimal.valueOf(10.00))
                .availability(true)
                .build();
    }

    @Test
    void getSimilarProducts_shouldReturnOkWithProducts() {
        final String productId = "1";
        final List<Product> products = List.of(buildProduct("2"), buildProduct("3"));
        final List<ProductResponseDto> responseDtos = List.of(
                buildProductResponseDto("2"), buildProductResponseDto("3"));

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(Mono.just(products));
        when(productResponseMapper.toProductResponseList(products))
                .thenReturn(responseDtos);

        StepVerifier.create(controller.getSimilarProducts(productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals(2, response.getBody().size());
                    assertEquals("2", response.getBody().get(0).getId());
                    assertEquals("3", response.getBody().get(1).getId());
                })
                .verifyComplete();

        verify(getSimilarProductsUseCase).getSimilarProducts(productId);
        verify(productResponseMapper).toProductResponseList(products);
    }

    @Test
    void getSimilarProducts_shouldReturnOkWithEmptyList() {
        final String productId = "1";
        final List<Product> products = List.of();
        final List<ProductResponseDto> responseDtos = List.of();

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(Mono.just(products));
        when(productResponseMapper.toProductResponseList(products))
                .thenReturn(responseDtos);

        StepVerifier.create(controller.getSimilarProducts(productId))
                .assertNext(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertTrue(response.getBody().isEmpty());
                })
                .verifyComplete();
    }

    @Test
    void getSimilarProducts_shouldPropagateError() {
        final String productId = "1";

        when(getSimilarProductsUseCase.getSimilarProducts(productId))
                .thenReturn(Mono.error(new RuntimeException("error")));

        StepVerifier.create(controller.getSimilarProducts(productId))
                .verifyError(RuntimeException.class);
    }
}
