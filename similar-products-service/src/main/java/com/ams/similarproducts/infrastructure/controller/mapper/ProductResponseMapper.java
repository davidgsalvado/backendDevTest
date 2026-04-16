package com.ams.similarproducts.infrastructure.controller.mapper;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.infrastructure.controller.dto.ProductResponseDto;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(config = CommonMapperConfiguration.class)
public interface ProductResponseMapper {

    ProductResponseDto toProductResponse(Product product);

    List<ProductResponseDto> toProductResponseList(List<Product> products);

}
