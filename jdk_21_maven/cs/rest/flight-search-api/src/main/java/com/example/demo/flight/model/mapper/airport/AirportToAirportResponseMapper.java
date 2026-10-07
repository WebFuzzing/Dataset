package com.example.demo.flight.model.mapper.airport;

import com.example.demo.common.model.mapper.BaseMapper;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.response.airport.AirportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting a {@link Airport} to a {@link AirportResponse}.
 * This interface extends the {@link BaseMapper} interface, enabling automatic mapping
 * between {@link Airport} and {@link AirportResponse} objects.
 */
@Mapper
public interface AirportToAirportResponseMapper extends BaseMapper<Airport, AirportResponse> {

    /**
     * Initializes and returns an instance of the {@link AirportToAirportResponseMapper}.
     * This method is used to obtain a mapper instance for converting a {@link Airport}
     * object into a {@link AirportResponse} object.
     *
     * @return an instance of the {@link AirportToAirportResponseMapper} mapper
     */
    static AirportToAirportResponseMapper initialize() {
        return Mappers.getMapper(AirportToAirportResponseMapper.class);
    }

}
