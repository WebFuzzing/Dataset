package com.example.demo.flight.model.mapper.flight;

import com.example.demo.common.model.mapper.BaseMapper;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.entity.FlightEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting a {@link FlightEntity} to a {@link Flight}.
 * This interface extends the {@link BaseMapper} interface, enabling automatic mapping
 * between {@link FlightEntity} and {@link Flight} objects.
 */
@Mapper
public interface FlightEntityToFlightMapper extends BaseMapper<FlightEntity, Flight> {

    /**
     * Initializes and returns an instance of the {@link FlightEntityToFlightMapper}.
     *
     * @return an instance of the mapper
     */
    static FlightEntityToFlightMapper initialize() {
        return Mappers.getMapper(FlightEntityToFlightMapper.class);
    }

}
