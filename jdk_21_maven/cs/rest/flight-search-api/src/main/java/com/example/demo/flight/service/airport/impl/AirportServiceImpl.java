package com.example.demo.flight.service.airport.impl;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;
import com.example.demo.flight.service.airport.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation for managing an airport in the system.
 */
@Service
@RequiredArgsConstructor
public class AirportServiceImpl implements AirportService {

    private final AirportCreateService airportCreateService;
    private final AirportReadService airportReadService;
    private final AirportUpdateService airportUpdateService;
    private final AirportDeleteService airportDeleteService;

    /**
     * Create a new airport to the database.
     *
     * @param createAirportRequest the request object containing the details of the CreateAirportRequest to be saved.
     * @return the saved {@link Airport} domain object.
     */
    @Override
    public Airport createAirport(CreateAirportRequest createAirportRequest) {
        return airportCreateService.createAirport(createAirportRequest);
    }

    /**
     * Retrieves an airport by its ID.
     *
     * @param id the ID of the airport to be retrieved.
     * @return the {@link Airport} entity with the specified ID.
     */
    @Override
    public Airport getAirportById(String id) {
        return airportReadService.getAirportById(id);
    }

    /**
     * Retrieves all airports with pagination support.
     *
     * @param customPagingRequest the request object containing paging parameters (e.g., page size, page number).
     * @return a {@link CustomPage} of airports, containing a list of airports and pagination details.
     */
    @Override
    public CustomPage<Airport> getAllAirports(CustomPagingRequest customPagingRequest) {
        return airportReadService.getAllAirports(customPagingRequest);
    }

    /**
     * Updates an existing an airport by its ID.
     *
     * @param id the ID of the airport to be updated.
     * @param updateAirportRequest the request object containing the updated details of the airport.
     * @return the updated {@link Airport} entity.
     */
    @Override
    public Airport updateAirportById(String id, UpdateAirportRequest updateAirportRequest) {
        return airportUpdateService.updateAirportById(id,updateAirportRequest);
    }

    /**
     * Deletes an airport by its ID.
     *
     * @param id the ID of the airport to be deleted.
     */
    @Override
    public void deleteAirportById(String id) {
        airportDeleteService.deleteAirportById(id);
    }


}
