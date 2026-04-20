package com.ams.similarproducts.infrastructure.api.mapper;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.infrastructure.api.dto.ProductDetailDto;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ProductApiMapperTest {

    private final ProductApiMapper mapper = Mappers.getMapper(ProductApiMapper.class);

    private static Stream<Arguments> parametersToDomain() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of(
                        getProduct(),
                        getProductDetailDto()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parametersToDomain")
    void toDomain(Product expected, ProductDetailDto productDetailDto) {
        final Product result = this.mapper.toDomain(productDetailDto);

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

    private static ProductDetailDto getProductDetailDto() {
        return ProductDetailDto.builder()
                .id("1")
                .name("Product 1")
                .price(BigDecimal.valueOf(19.99))
                .availability(true)
                .build();
    }
}
