package com.example.demo.flight.service.flight;

/**
 * Service interface for deleting a flight in the system.
 */
public interface FlightDeleteService {

    /**
     * Deletes a flight by its ID.
     *
     * @param id the ID of the flight to be deleted.
     */
    void deleteFlightById(String id);

}
