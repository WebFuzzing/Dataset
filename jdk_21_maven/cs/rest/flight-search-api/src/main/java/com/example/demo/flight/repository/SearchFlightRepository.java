package com.example.demo.flight.repository;

import com.example.demo.flight.model.entity.FlightEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;

/**
 * Repository interface for searching flights.
 */
public interface SearchFlightRepository extends MongoRepository<FlightEntity, String> {

    /**
     * Finds flights based on the specified criteria and returns a paginated result.
     *
     * @param fromAirportId the ID of the departure airport.
     * @param toAirportId the ID of the destination airport.
     * @param start the start of the departure time range.
     * @param end the end of the departure time range.
     * @param pageable pagination details.
     * @return a {@link Page} of {@link FlightEntity} matching the search criteria.
     */
    @Query("{ 'fromAirport.id': ?0, 'toAirport.id': ?1, 'departureTime': { $gte: ?2, $lte: ?3 } }")
    Page<FlightEntity> findFlights(
            String fromAirportId,
            String toAirportId,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable);

}
