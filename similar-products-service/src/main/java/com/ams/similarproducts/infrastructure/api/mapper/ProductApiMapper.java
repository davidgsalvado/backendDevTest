package com.ams.similarproducts.infrastructure.api.mapper;

import com.ams.similarproducts.domain.entity.Product;
import com.ams.similarproducts.infrastructure.api.dto.ProductDetailDto;
import org.mapstruct.Mapper;

@Mapper(config = CommonMapperConfiguration.class)
public interface ProductApiMapper {

    Product toDomain(ProductDetailDto productDetailDto);

}
