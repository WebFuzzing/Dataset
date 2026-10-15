package com.example.demo.flight.model.mapper.flight;

import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.entity.FlightEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface that converts a list of {@link FlightEntity} objects to a list of {@link Flight} objects.
 * It leverages MapStruct for automatic mapping between entity and domain objects.
 */
@Mapper
public interface ListFlightEntityToListFlightMapper {

    FlightEntityToFlightMapper flightEntityToFlightMapper = Mappers.getMapper(FlightEntityToFlightMapper.class);

    /**
     * Converts a list of {@link FlightEntity} objects to a list of {@link Flight} objects.
     *
     * @param flightsEntities the list of {@link FlightEntity} objects to be converted
     * @return a list of {@link Flight} objects, or {@code null} if {@code flightsEntities} is {@code null}
     */
    default List<Flight> toFlightList(List<FlightEntity> flightsEntities) {

        if (flightsEntities == null) {
            return null;
        }

        return flightsEntities.stream()
                .map(flightEntityToFlightMapper::map)
                .collect(Collectors.toList());

    }

    /**
     * Initializes and returns an instance of the {@link ListFlightEntityToListFlightMapper}.
     *
     * @return an instance of the mapper
     */
    static ListFlightEntityToListFlightMapper initialize() {
        return Mappers.getMapper(ListFlightEntityToListFlightMapper.class);
    }

}
