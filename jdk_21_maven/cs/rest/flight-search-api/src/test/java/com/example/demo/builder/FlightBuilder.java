package com.example.demo.builder;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.Flight;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A builder class for creating instances of {@link Flight} with specific properties set.
 * This builder is intended for creating instances of {@link Flight} with specific configurations.
 */
public class FlightBuilder extends BaseBuilder<Flight> {

    public FlightBuilder() {
        super(Flight.class);
    }

    public FlightBuilder withValidFields() {
        return this
                .withId(UUID.randomUUID().toString())
                .withFromAirport(new AirportBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withName("Origin Airport")
                        .withCityName("Origin City")
                        .build())
                .withToAirport(new AirportBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withName("Destination Airport")
                        .withCityName("Destination City")
                        .build())
                .withDepartureTime(LocalDateTime.now().plusDays(1))
                .withArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .withPrice(150.0);

    }

    public FlightBuilder withId(String id) {
        data.setId(id);
        return this;
    }

    public FlightBuilder withFromAirport(Airport fromAirport) {
        data.setFromAirport(fromAirport);
        return this;
    }

    public FlightBuilder withToAirport(Airport toAirport) {
        data.setToAirport(toAirport);
        return this;
    }

    public FlightBuilder withDepartureTime(LocalDateTime departureTime) {
        data.setDepartureTime(departureTime);
        return this;
    }

    public FlightBuilder withArrivalTime(LocalDateTime arrivalTime) {
        data.setArrivalTime(arrivalTime);
        return this;
    }

    public FlightBuilder withPrice(Double price) {
        data.setPrice(price);
        return this;
    }

}

