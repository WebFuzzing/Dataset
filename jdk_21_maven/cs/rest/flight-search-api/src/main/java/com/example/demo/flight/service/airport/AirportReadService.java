package com.example.demo.flight.service.airport;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.model.Airport;

/**
 * Service interface for retrieving an airport in the system.
 */
public interface AirportReadService {

    /**
     * Retrieves an airport by its ID.
     *
     * @param id the ID of the task to be retrieved.
     * @return the {@link Airport} entity with the specified ID.
     */
    Airport getAirportById(final String id);

    /**
     * Retrieves all airports with pagination support.
     *
     * @param customPagingRequest the request object containing paging parameters (e.g., page size, page number).
     * @return a {@link CustomPage} of airports, containing a list of airports and pagination details.
     */
    CustomPage<Airport> getAllAirports(final CustomPagingRequest customPagingRequest);

}
