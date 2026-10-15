package com.example.demo.flight.model.mapper.airport;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.entity.AirportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface that converts a list of {@link AirportEntity} objects to a list of {@link Airport} objects.
 * It leverages MapStruct for automatic mapping between entity and domain objects.
 */
@Mapper
public interface ListAirportEntityToListAirportMapper {

    AirportEntityToAirportMapper airportEntityToAirportMapper = Mappers.getMapper(AirportEntityToAirportMapper.class);

    /**
     * Converts a list of {@link AirportEntity} objects to a list of {@link Airport} objects.
     *
     * @param airportEntities the list of {@link AirportEntity} objects to be converted
     * @return a list of {@link Airport} objects, or {@code null} if {@code airportEntities} is {@code null}
     */
    default List<Airport> toAirportList(List<AirportEntity> airportEntities) {

        if (airportEntities == null) {
            return null;
        }

        return airportEntities.stream()
                .map(airportEntityToAirportMapper::map)
                .collect(Collectors.toList());

    }

    /**
     * Initializes and returns an instance of the {@link ListAirportEntityToListAirportMapper}.
     *
     * @return an instance of the mapper
     */
    static ListAirportEntityToListAirportMapper initialize() {
        return Mappers.getMapper(ListAirportEntityToListAirportMapper.class);
    }

}
