package com.example.joinUs.mapping;

import com.example.joinUs.dto.VenueDTO;
import com.example.joinUs.model.mongodb.Venue;
import org.mapstruct.Mapper;


@Mapper(config = CentralMappingConfig.class, uses = { CityMapper.class })
public interface VenueMapper {

    VenueDTO toDTO(Venue venue);

    Venue toEntity(VenueDTO dto);

}
