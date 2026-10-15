package com.example.demo.builder;

import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A builder class for creating instances of {@link UpdateFlightRequestBuilder} with specific properties set.
 */
public class UpdateFlightRequestBuilder extends BaseBuilder<UpdateFlightRequest> {

    public UpdateFlightRequestBuilder() {
        super(UpdateFlightRequest.class);
    }

    public UpdateFlightRequestBuilder withValidFields() {
        return this.withFromAirportId(UUID.randomUUID().toString())
                .withToAirportId(UUID.randomUUID().toString())
                .withDepartureTime(LocalDateTime.now().plusDays(1))
                .withArrivalTime(LocalDateTime.now().plusDays(2))
                .withPrice(250.0);
    }

    public UpdateFlightRequestBuilder withFromAirportId(String fromAirportId) {
        data.setFromAirportId(fromAirportId);
        return this;
    }

    public UpdateFlightRequestBuilder withToAirportId(String toAirportId) {
        data.setToAirportId(toAirportId);
        return this;
    }

    public UpdateFlightRequestBuilder withDepartureTime(LocalDateTime departureTime) {
        data.setDepartureTime(departureTime);
        return this;
    }

    public UpdateFlightRequestBuilder withArrivalTime(LocalDateTime arrivalTime) {
        data.setArrivalTime(arrivalTime);
        return this;
    }

    public UpdateFlightRequestBuilder withPrice(Double price) {
        data.setPrice(price);
        return this;
    }

}

