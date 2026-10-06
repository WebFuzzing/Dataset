package com.example.demo.flight.model.mapper.flight;

import com.example.demo.common.model.mapper.BaseMapper;
import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.airport.CreateAirportRequestToAirportEntityMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting a {@link CreateFlightRequest} to a {@link FlightEntity}.
 * This interface extends the {@link BaseMapper} interface, allowing automatic mapping
 * between the {@link CreateFlightRequest} and {@link FlightEntity} objects.
 */
@Mapper
public interface CreateFlightRequestToFlightEntityMapper extends BaseMapper<CreateFlightRequest, FlightEntity> {

    /**
     * Converts a {@link CreateFlightRequest} to a {@link FlightEntity}.
     * This method is used for saving a flight, mapping only the necessary fields from the
     * {@link CreateFlightRequest} to the {@link FlightEntity}.
     *
     * @param request the {@link CreateFlightRequest} to be mapped
     * @param fromAirport the from airport entity
     * @param toAirport the to airport entity
     * @return the resulting {@link FlightEntity} containing the mapped fields
     */
    @Named("mapForSaving")
    default FlightEntity mapForSaving(CreateFlightRequest request, AirportEntity fromAirport, AirportEntity toAirport) {
        return FlightEntity.builder()
                .fromAirport(fromAirport)
                .toAirport(toAirport)
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .price(request.getPrice())
                .build();
    }

    /**
     * Initializes and returns an instance of the {@link CreateAirportRequestToAirportEntityMapper}.
     *
     * @return an instance of the mapper
     */
    static CreateFlightRequestToFlightEntityMapper initialize(){
        return Mappers.getMapper(CreateFlightRequestToFlightEntityMapper.class);
    }


}
