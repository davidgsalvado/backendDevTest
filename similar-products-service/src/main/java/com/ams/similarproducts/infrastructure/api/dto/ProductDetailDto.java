package com.ams.similarproducts.infrastructure.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProductDetailDto {

    private String id;
    private String name;
    private BigDecimal price;
    private Boolean availability;

}
