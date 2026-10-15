package com.example.demo.flight.model.mapper.airport;

import com.example.demo.common.model.mapper.BaseMapper;
import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting a {@link CreateAirportRequest} to a {@link AirportEntity}.
 * This interface extends the {@link BaseMapper} interface, allowing automatic mapping
 * between the {@link CreateAirportRequest} and {@link AirportEntity} objects.
 */
@Mapper
public interface CreateAirportRequestToAirportEntityMapper extends BaseMapper<CreateAirportRequest, AirportEntity> {

    /**
     * Converts a {@link CreateAirportRequest} to a {@link AirportEntity}.
     * This method is used for saving an airport, mapping only the necessary fields from the
     * {@link CreateAirportRequest} to the {@link AirportEntity}.
     *
     * @param request the {@link CreateAirportRequest} to be mapped
     * @return the resulting {@link AirportEntity} containing the mapped fields
     */
    @Named("mapForSaving")
    default AirportEntity mapForSaving(CreateAirportRequest request){
        return AirportEntity.builder()
                .name(request.getName())
                .cityName(request.getCityName())
                .build();
    }

    /**
     * Initializes and returns an instance of the {@link CreateAirportRequestToAirportEntityMapper}.
     *
     * @return an instance of the mapper
     */
    static CreateAirportRequestToAirportEntityMapper initialize(){
        return Mappers.getMapper(CreateAirportRequestToAirportEntityMapper.class);
    }

}
