package com.example.demo.flight.service.flight;

import com.example.demo.common.model.CustomPage;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.SearchFlightRequest;

/**
 * Service interface for search operations.
 */
public interface SearchFlightService {

    /**
     * Searches for flights based on the given criteria and returns a paginated result.
     *
     * @param request the search criteria including airport IDs, departure, and optional return times.
     * @return a {@link CustomPage} of {@link Flight} with matching flights and pagination details.
     */
    CustomPage<Flight> searchFlights(SearchFlightRequest request);

}
