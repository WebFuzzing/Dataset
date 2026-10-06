package com.example.demo.flight.service.airport;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;

/**
 * Service interface for managing an airport in the system.
 */
public interface AirportService {

    /**
     * Create a new airport to the database.
     *
     * @param createAirportRequest the request object containing the details of the CreateAirportRequest to be saved.
     * @return the saved {@link Airport} domain object.
     */
    Airport createAirport(CreateAirportRequest createAirportRequest);

    /**
     * Retrieves an airport by its ID.
     *
     * @param id the ID of the airport to be retrieved.
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

    /**
     * Updates an existing an airport by its ID.
     *
     * @param id the ID of the airport to be updated.
     * @param updateAirportRequest the request object containing the updated details of the airport.
     * @return the updated {@link Airport} entity.
     */
    Airport updateAirportById(final String id, final UpdateAirportRequest updateAirportRequest);

    /**
     * Deletes an airport by its ID.
     *
     * @param id the ID of the airport to be deleted.
     */
    void deleteAirportById(String id);


}
