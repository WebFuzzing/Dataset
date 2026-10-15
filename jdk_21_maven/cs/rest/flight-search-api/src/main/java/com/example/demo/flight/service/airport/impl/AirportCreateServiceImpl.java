package com.example.demo.flight.service.airport.impl;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.mapper.airport.AirportEntityToAirportMapper;
import com.example.demo.flight.model.mapper.airport.CreateAirportRequestToAirportEntityMapper;
import com.example.demo.flight.repository.AirportRepository;
import com.example.demo.flight.service.airport.AirportCreateService;
import com.example.demo.flight.utils.AirportUtilityClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation for creating an airport in the system.
 */
@Service
@RequiredArgsConstructor
public class AirportCreateServiceImpl implements AirportCreateService {

    private final AirportRepository airportRepository;

    private final CreateAirportRequestToAirportEntityMapper createAirportRequestToAirportEntityMapper =
            CreateAirportRequestToAirportEntityMapper.initialize();

    private final AirportEntityToAirportMapper airportEntityToAirportMapper =
            AirportEntityToAirportMapper.initialize();

    /**
     * Create a new airport to the database.
     *
     * @param createAirportRequest the request object containing the details of the CreateAirportRequest to be saved.
     * @return the saved {@link Airport} domain object.
     */
    @Override
    public Airport createAirport(CreateAirportRequest createAirportRequest) {

        AirportUtilityClass.checkAirportNameUniqueness(airportRepository, createAirportRequest.getName());

        AirportEntity airportEntityToBeSaved = createAirportRequestToAirportEntityMapper.mapForSaving(createAirportRequest);

        AirportEntity savedAirportEntity = airportRepository.save(airportEntityToBeSaved);

        return airportEntityToAirportMapper.map(savedAirportEntity);

    }

}
