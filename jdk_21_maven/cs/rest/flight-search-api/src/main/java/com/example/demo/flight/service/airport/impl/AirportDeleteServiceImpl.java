package com.example.demo.flight.service.airport.impl;

import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.repository.AirportRepository;
import com.example.demo.flight.service.airport.AirportDeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation for deleting an airport in the system.
 */
@Service
@RequiredArgsConstructor
public class AirportDeleteServiceImpl implements AirportDeleteService {

    private final AirportRepository airportRepository;

    /**
     * Deletes an airport by its ID.
     *
     * @param id the ID of the airport to be deleted.
     */
    @Override
    public void deleteAirportById(String id) {

        AirportEntity airportEntityToBeDeleted = airportRepository.findById(id)
                .orElseThrow(()->new AirportNotFoundException("With given id = " + id));

        airportRepository.delete(airportEntityToBeDeleted);

    }

}
