package com.ams.similarproducts.infrastructure.controller.mapper;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.infrastructure.controller.dto.ProductResponseDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProductResponseMapperTest {

    private final ProductResponseMapper mapper = Mappers.getMapper(ProductResponseMapper.class);

    private static Stream<Arguments> parametersToProductResponse() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(
                        getProductResponseDto(),
                        getProduct()
                )
        );
    }

    private static Stream<Arguments> parametersToProductResponseList() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(List.of(), List.of()),
                Arguments.of(
                        List.of(getProductResponseDto()),
                        List.of(getProduct())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parametersToProductResponse")
    void toProductResponse(ProductResponseDto expected, Product product) {
        final ProductResponseDto result = this.mapper.toProductResponse(product);

        assertEquals(expected, result);
    }

    @ParameterizedTest
    @MethodSource("parametersToProductResponseList")
    void toProductResponseList(List<ProductResponseDto> expected, List<Product> products) {
        final List<ProductResponseDto> result = this.mapper.toProductResponseList(products);

        assertEquals(expected, result);
    }

    private static Product getProduct() {
        return Product.builder()
                .id("1")
                .name("Product 1")
                .price(BigDecimal.valueOf(19.99))
                .availability(true)
                .build();
    }

    private static ProductResponseDto getProductResponseDto() {
        return ProductResponseDto.builder()
                .id("1")
                .name("Product 1")
                .price(BigDecimal.valueOf(19.99))
                .availability(true)
                .build();
    }
}
