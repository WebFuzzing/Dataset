package com.example.demo.flight.model.mapper.flight;

import com.example.demo.common.model.mapper.BaseMapper;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting and updating a {@link FlightEntity} using data from an {@link UpdateFlightRequest}.
 * This interface extends the {@link BaseMapper} interface, enabling automatic mapping
 * between {@link UpdateFlightRequest} and {@link FlightEntity}.
 */
@Mapper
public interface UpdateFlightRequestToFlightEntityMapper extends BaseMapper<UpdateFlightRequest, FlightEntity> {

    /**
     * Updates the fields of an existing {@link FlightEntity} with data from an {@link UpdateFlightRequest}.
     * This method maps the fields from the request to the entity, specifically
     * updating flight details such as airports, times, and price.
     *
     * @param flightEntity the {@link FlightEntity} to be updated
     * @param updateFlightRequest the {@link UpdateFlightRequest} containing the new data
     */
    @Named("mapForUpdate")
    default void updateFlightMapper(final FlightEntity flightEntity, final UpdateFlightRequest updateFlightRequest,
                                    AirportEntity departureAirportEntity, AirportEntity arrivalAirportEntity) {
        flightEntity.setFromAirport(AirportEntity.builder()
                .id(updateFlightRequest.getFromAirportId())
                .name(departureAirportEntity.getName())
                .cityName(departureAirportEntity.getCityName())
                .build());

        flightEntity.setToAirport(AirportEntity.builder()
                .id(updateFlightRequest.getToAirportId())
                .name(arrivalAirportEntity.getName())
                .cityName(arrivalAirportEntity.getCityName())
                .build());

        flightEntity.setDepartureTime(updateFlightRequest.getDepartureTime());
        flightEntity.setArrivalTime(updateFlightRequest.getArrivalTime());
        flightEntity.setPrice(updateFlightRequest.getPrice());
    }

    /**
     * Initializes and returns an instance of the {@link UpdateFlightRequestToFlightEntityMapper}.
     * This method is used to obtain a mapper instance for mapping an {@link UpdateFlightRequest}
     * to a {@link FlightEntity}, including updating an existing entity.
     *
     * @return an instance of the {@link UpdateFlightRequestToFlightEntityMapper} mapper
     */
    static UpdateFlightRequestToFlightEntityMapper initialize() {
        return Mappers.getMapper(UpdateFlightRequestToFlightEntityMapper.class);
    }

}

