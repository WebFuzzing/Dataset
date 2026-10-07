package com.example.demo.builder;


import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;

import java.time.LocalDateTime;

/**
 * A builder class for creating instances of {@link CreateFlightRequest} with specific properties set.
 */
public class CreateFlightRequestBuilder extends BaseBuilder<CreateFlightRequest> {

    public CreateFlightRequestBuilder() { super(CreateFlightRequest.class);}

    public CreateFlightRequestBuilder withValidFields() {
        return this.withFromAirportId("AIRPORT1")
                .withToAirportId("AIRPORT2")
                .withDepartureTime(LocalDateTime.of(2025, 1, 13, 10, 0))
                .withArrivalTime(LocalDateTime.of(2025, 1, 13, 12, 0))
                .withPrice(100.0);
    }

    public CreateFlightRequestBuilder withFromAirportId(String fromAirportId) {
        data.setFromAirportId(fromAirportId);
        return this;
    }

    public CreateFlightRequestBuilder withToAirportId(String toAirportId) {
        data.setToAirportId(toAirportId);
        return this;
    }

    public CreateFlightRequestBuilder withDepartureTime(LocalDateTime departureTime) {
        data.setDepartureTime(departureTime);
        return this;
    }

    public CreateFlightRequestBuilder withArrivalTime(LocalDateTime arrivalTime) {
        data.setArrivalTime(arrivalTime);
        return this;
    }

    public CreateFlightRequestBuilder withPrice(Double price) {
        data.setPrice(price);
        return this;
    }


}
