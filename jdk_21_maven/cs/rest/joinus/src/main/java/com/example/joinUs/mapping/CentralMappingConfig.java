package com.example.joinUs.mapping;

import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;


@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface CentralMappingConfig {

}
