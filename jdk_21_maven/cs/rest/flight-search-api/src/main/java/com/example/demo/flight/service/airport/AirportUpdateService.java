package com.example.demo.flight.service.airport;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;

/**
 * Service interface for updating an airport in the system.
 */
public interface AirportUpdateService {

    /**
     * Updates an existing an airport by its ID.
     *
     * @param id the ID of the airport to be updated.
     * @param updateAirportRequest the request object containing the updated details of the airport.
     * @return the updated {@link Airport} entity.
     */
    Airport updateAirportById(final String id, final UpdateAirportRequest updateAirportRequest);

}
