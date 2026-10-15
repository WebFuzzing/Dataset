package com.example.demo.builder;

import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A builder class for creating instances of {@link FlightEntity} with specific properties set.
 * This builder is intended for creating instances of {@link FlightEntity} with specific configurations.
 */
public class FlightEntityBuilder extends BaseBuilder<FlightEntity> {

    public FlightEntityBuilder() {
        super(FlightEntity.class);
    }

    public FlightEntityBuilder withValidFields() {
        return this
                .withId(UUID.randomUUID().toString())
                .withFromAirport(new AirportEntityBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withName("Origin Airport")
                        .withCityName("Origin City")
                        .build())
                .withToAirport(new AirportEntityBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withName("Destination Airport")
                        .withCityName("Destination City")
                        .build())
                .withDepartureTime(LocalDateTime.now().plusDays(1))
                .withArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .withPrice(150.0);
    }

    public FlightEntityBuilder withId(String id) {
        data.setId(id);
        return this;
    }

    public FlightEntityBuilder withFromAirport(AirportEntity fromAirport) {
        data.setFromAirport(fromAirport);
        return this;
    }

    public FlightEntityBuilder withToAirport(AirportEntity toAirport) {
        data.setToAirport(toAirport);
        return this;
    }

    public FlightEntityBuilder withDepartureTime(LocalDateTime departureTime) {
        data.setDepartureTime(departureTime);
        return this;
    }

    public FlightEntityBuilder withArrivalTime(LocalDateTime arrivalTime) {
        data.setArrivalTime(arrivalTime);
        return this;
    }

    public FlightEntityBuilder withPrice(Double price) {
        data.setPrice(price);
        return this;
    }

}

