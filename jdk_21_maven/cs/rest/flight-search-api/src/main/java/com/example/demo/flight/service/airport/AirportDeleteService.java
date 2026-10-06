package com.example.demo.flight.service.airport;

/**
 * Service interface for deleting an airport in the system.
 */
public interface AirportDeleteService {

    /**
     * Deletes an airport by its ID.
     *
     * @param id the ID of the airport to be deleted.
     */
    void deleteAirportById(String id);

}
