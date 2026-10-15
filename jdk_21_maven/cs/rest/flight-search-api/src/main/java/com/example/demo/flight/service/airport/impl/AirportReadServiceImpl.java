package com.example.demo.flight.service.airport.impl;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.mapper.airport.AirportEntityToAirportMapper;
import com.example.demo.flight.model.mapper.airport.ListAirportEntityToListAirportMapper;
import com.example.demo.flight.repository.AirportRepository;
import com.example.demo.flight.service.airport.AirportReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for retrieving an airport in the system.
 */
@Service
@RequiredArgsConstructor
public class AirportReadServiceImpl implements AirportReadService {

    private final AirportRepository airportRepository;

    private final AirportEntityToAirportMapper airportEntityToAirportMapper =
            AirportEntityToAirportMapper.initialize();

    private final ListAirportEntityToListAirportMapper listAirportEntityToListAirportMapper =
            ListAirportEntityToListAirportMapper.initialize();

    /**
     * Retrieves an airport by its ID.
     *
     * @param id the ID of the task to be retrieved.
     * @return the {@link Airport} entity with the specified ID.
     */
    @Override
    public Airport getAirportById(String id) {

        AirportEntity airportEntityFromDb = airportRepository.findById(id)
                .orElseThrow(()->new AirportNotFoundException("Airport given id cant found"));

        return airportEntityToAirportMapper.map(airportEntityFromDb);

    }

    /**
     * Retrieves all airports with pagination support.
     *
     * @param customPagingRequest the request object containing paging parameters (e.g., page size, page number).
     * @return a {@link CustomPage} of tasks, containing a list of airports and pagination details.
     */
    @Override
    public CustomPage<Airport> getAllAirports(CustomPagingRequest customPagingRequest) {

        Page<AirportEntity> taskEntitiesListPage = airportRepository.findAll(customPagingRequest.toPageable());

        if (taskEntitiesListPage.getContent().isEmpty()) {
            throw new AirportNotFoundException("Couldn't find any airport");
        }

        final List<Airport> productDomainModels = listAirportEntityToListAirportMapper
                .toAirportList(taskEntitiesListPage.getContent());

        return CustomPage.of(productDomainModels, taskEntitiesListPage);

    }

}
