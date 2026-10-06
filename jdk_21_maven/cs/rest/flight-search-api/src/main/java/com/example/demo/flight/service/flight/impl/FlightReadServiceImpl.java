package com.example.demo.flight.service.flight.impl;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.exception.FlightNotFoundException;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.FlightEntityToFlightMapper;
import com.example.demo.flight.model.mapper.flight.ListFlightEntityToListFlightMapper;
import com.example.demo.flight.repository.FlightRepository;
import com.example.demo.flight.service.flight.FlightReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service implementation for retrieving a flight in the system.
 */
@Service
@RequiredArgsConstructor
public class FlightReadServiceImpl implements FlightReadService {

    private final FlightRepository flightRepository;

    private final FlightEntityToFlightMapper flightEntityToFlightMapper =
            FlightEntityToFlightMapper.initialize();

    private final ListFlightEntityToListFlightMapper listFlightEntityToListFlightMapper =
            ListFlightEntityToListFlightMapper.initialize();

    /**
     * Retrieves a flight by its ID.
     *
     * @param id the ID of the flight to be retrieved.
     * @return the {@link Flight} entity with the specified ID.
     */
    @Override
    public Flight getFlightById(String id) {

        FlightEntity flightEntityFromDb = flightRepository.findById(id)
                .orElseThrow(()->new FlightNotFoundException("Flight given id " + id + " +can't found "));

        return flightEntityToFlightMapper.map(flightEntityFromDb);

    }

    /**
     * Retrieves all flights with pagination support.
     *
     * @param customPagingRequest the request object containing paging parameters (e.g., page size, page number).
     * @return a {@link CustomPage} of flights, containing a list of flights and pagination details.
     */
    @Override
    public CustomPage<Flight> getAllFlights(CustomPagingRequest customPagingRequest) {

        Page<FlightEntity> flightEntitiesListPage = flightRepository.findAll(customPagingRequest.toPageable());

        if (flightEntitiesListPage.getContent().isEmpty()) {
            throw new FlightNotFoundException("Couldn't find any airport");
        }

        final List<Flight> fligtDomainList = listFlightEntityToListFlightMapper
                .toFlightList(flightEntitiesListPage.getContent());

        return CustomPage.of(fligtDomainList, flightEntitiesListPage);

    }


}
