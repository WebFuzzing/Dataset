package com.example.demo.flight.service.flight;

import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;

/**
 * Service interface for updating a flight in the system.
 */
public interface FlightUpdateService {

    /**
     * Updates an existing a flight by its ID.
     *
     * @param id the ID of the flight to be updated.
     * @param updateFlightRequest the request object containing the updated details of the flight.
     * @return the updated {@link Flight} entity.
     */
    Flight updateFlightById(final String id, final UpdateFlightRequest updateFlightRequest);

}
