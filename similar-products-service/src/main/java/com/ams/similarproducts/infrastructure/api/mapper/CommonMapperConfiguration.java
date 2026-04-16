package com.ams.similarproducts.infrastructure.api.mapper;

import org.mapstruct.MapperConfig;

@MapperConfig(nullValueCheckStrategy = org.mapstruct.NullValueCheckStrategy.ALWAYS,
             nullValueMappingStrategy = org.mapstruct.NullValueMappingStrategy.RETURN_NULL,
             injectionStrategy = org.mapstruct.InjectionStrategy.CONSTRUCTOR,
             unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE,
             nullValuePropertyMappingStrategy = org.mapstruct.NullValuePropertyMappingStrategy.SET_TO_NULL)
public interface CommonMapperConfiguration {
}
