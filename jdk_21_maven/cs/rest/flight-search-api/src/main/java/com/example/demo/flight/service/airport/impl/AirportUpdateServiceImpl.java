package com.example.demo.flight.service.airport.impl;

import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.mapper.airport.AirportEntityToAirportMapper;
import com.example.demo.flight.model.mapper.airport.UpdateAirportRequestToAirportEntityMapper;
import com.example.demo.flight.repository.AirportRepository;
import com.example.demo.flight.service.airport.AirportUpdateService;
import com.example.demo.flight.utils.AirportUtilityClass;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation for updating an airport in the system.
 */
@Service
@RequiredArgsConstructor
public class AirportUpdateServiceImpl implements AirportUpdateService {

    private final AirportRepository airportRepository;

    private final UpdateAirportRequestToAirportEntityMapper updateAirportRequestToAirportEntityMapper =
            UpdateAirportRequestToAirportEntityMapper.initialize();

    private final AirportEntityToAirportMapper airportEntityToAirportMapper =
            AirportEntityToAirportMapper.initialize();

    /**
     * Updates an existing an airport by its ID.
     *
     * @param id the ID of the airport to be updated.
     * @param updateAirportRequest the request object containing the updated details of the airport.
     * @return the updated {@link Airport} entity.
     */
    @Override
    public Airport updateAirportById(String id, UpdateAirportRequest updateAirportRequest) {

        AirportUtilityClass.checkAirportNameUniqueness(airportRepository, updateAirportRequest.getName());

        AirportEntity airportEntity = airportRepository.findById(id)
                .orElseThrow(()->new AirportNotFoundException("Airport given id cant found" + id));

        updateAirportRequestToAirportEntityMapper.updateAirportMapper(airportEntity,updateAirportRequest);

        AirportEntity updatedAirportEntity = airportRepository.save(airportEntity);

        return airportEntityToAirportMapper.map(updatedAirportEntity);

    }

}
