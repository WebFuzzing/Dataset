package com.example.demo.flight.utils;

import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.repository.AirportRepository;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AirportUtilityClass {

    /**
     * Checks the uniqueness of an airport name in the repository.
     * If an airport with the given name already exists, a {@link AirportNameAlreadyExistException} is thrown.
     *
     * @param airportName the name of the airport to be checked for uniqueness.
     * @throws AirportNameAlreadyExistException if a task with the given name already exists in the repository.
     */
    public void checkAirportNameUniqueness(final AirportRepository airportRepository, final String airportName) {
        if (airportRepository.existsByName(airportName)) {
            throw new AirportNameAlreadyExistException("With given airport name = " + airportName);
        }
    }

}
