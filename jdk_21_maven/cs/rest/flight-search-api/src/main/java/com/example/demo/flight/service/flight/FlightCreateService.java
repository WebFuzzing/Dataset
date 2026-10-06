package com.example.demo.flight.service.flight;

import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;

/**
 * Service interface for creating a flight in the system.
 */
public interface FlightCreateService {

    /**
     * Creates a new flight in the system.
     *
     * @param createFlightRequest the request object containing the details of the flight to be created.
     * @return the created {@link Flight} entity.
     */
    Flight createFlight(CreateFlightRequest createFlightRequest);

}
