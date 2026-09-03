package com.example.joinUs.mapping;

import com.example.joinUs.dto.CityDTO;
import com.example.joinUs.model.embedded.CityEmbedded;
import com.example.joinUs.model.mongodb.City;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(config = CentralMappingConfig.class)
public interface CityMapper {

    CityDTO toDTO(City city);

    City toEntity(CityDTO dto);

    @Mapping(target = "cityId", source = "id")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "country", source = "country")
    @Mapping(target = "state", source = "state")
    CityEmbedded toEmbedded(City city);
}
