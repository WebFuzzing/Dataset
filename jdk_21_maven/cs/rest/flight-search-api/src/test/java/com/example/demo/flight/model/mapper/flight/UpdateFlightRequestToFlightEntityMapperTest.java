package com.example.demo.flight.model.mapper.flight;


import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpdateFlightRequestToFlightEntityMapper}.
 * This class tests the functionality of mapping between {@link UpdateFlightRequest}
 * and {@link FlightEntity} using the {@link UpdateFlightRequestToFlightEntityMapper}.
 */
class UpdateFlightRequestToFlightEntityMapperTest {

    private final UpdateFlightRequestToFlightEntityMapper mapper = UpdateFlightRequestToFlightEntityMapper.initialize();

    @Test
    void testMap_WhenUpdateFlightRequestIsNull() {

        FlightEntity result = mapper.map((UpdateFlightRequest) null);
        assertNull(result);

    }

    @Test
    void testMap_WhenUpdateFlightRequestIsValid() {

        // Arrange: Create an UpdateFlightRequest
        String fromAirportId = UUID.randomUUID().toString();
        String toAirportId = UUID.randomUUID().toString();
        UpdateFlightRequest request = UpdateFlightRequest.builder()
                .fromAirportId(fromAirportId)
                .toAirportId(toAirportId)
                .departureTime(LocalDateTime.of(2024, 1, 18, 10, 30))
                .arrivalTime(LocalDateTime.of(2024, 1, 18, 14, 45))
                .price(299.99)
                .build();

        // Act: Map the request to a FlightEntity
        FlightEntity result = mapper.map(request);

        // Assert: Validate the mapped FlightEntity
        assertNotNull(result, "Resulting FlightEntity should not be null");

        // Verify departureTime, arrivalTime, and price
        assertEquals(request.getDepartureTime(), result.getDepartureTime(), "Departure Time mismatch");
        assertEquals(request.getArrivalTime(), result.getArrivalTime(), "Arrival Time mismatch");
        assertEquals(request.getPrice(), result.getPrice(), "Price mismatch");

    }

    @Test
    void testMap_WhenUpdateFlightRequestIsEmpty() {

        UpdateFlightRequest updateFlightRequest = new UpdateFlightRequest(
                null, null, null, null, null
        );

        FlightEntity result = mapper.map(updateFlightRequest);

        assertNotNull(result);
        assertNull(result.getFromAirport());
        assertNull(result.getToAirport());
        assertNull(result.getDepartureTime());
        assertNull(result.getArrivalTime());
        assertNull(result.getPrice());

    }

    @Test
    void testMap_WhenUpdateFlightRequestCollectionIsNull() {

        List<UpdateFlightRequest> updateFlightRequests = null;

        List<FlightEntity> result = mapper.map(updateFlightRequests);

        assertNull(result);

    }

    @Test
    void testMap_WhenUpdateFlightRequestCollectionIsEmpty() {

        List<UpdateFlightRequest> updateFlightRequests = Collections.emptyList();

        List<FlightEntity> result = mapper.map(updateFlightRequests);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMap_WhenUpdateFlightRequestCollectionHasValidElements() {

        // Arrange: Create UpdateFlightRequest objects
        String fromAirportId1 = UUID.randomUUID().toString();
        String toAirportId1 = UUID.randomUUID().toString();
        UpdateFlightRequest updateFlightRequest1 = UpdateFlightRequest.builder()
                .fromAirportId(fromAirportId1)
                .toAirportId(toAirportId1)
                .departureTime(LocalDateTime.of(2024, 1, 18, 10, 30))
                .arrivalTime(LocalDateTime.of(2024, 1, 18, 14, 45))
                .price(299.99)
                .build();

        String fromAirportId2 = UUID.randomUUID().toString();
        String toAirportId2 = UUID.randomUUID().toString();
        UpdateFlightRequest updateFlightRequest2 = UpdateFlightRequest.builder()
                .fromAirportId(fromAirportId2)
                .toAirportId(toAirportId2)
                .departureTime(LocalDateTime.of(2024, 1, 19, 12, 15))
                .arrivalTime(LocalDateTime.of(2024, 1, 19, 16, 45))
                .price(350.75)
                .build();

        List<UpdateFlightRequest> updateFlightRequests = Arrays.asList(updateFlightRequest1, updateFlightRequest2);

        // Act: Map the list of UpdateFlightRequest objects to FlightEntity objects
        List<FlightEntity> result = mapper.map(updateFlightRequests);

        // Assert: Validate the results
        assertNotNull(result, "The resulting list of FlightEntity objects should not be null");
        assertEquals(2, result.size(), "The size of the resulting list should match the input list size");

        // Validate the first FlightEntity
        FlightEntity flight1 = result.get(0);
        assertNotNull(flight1, "First FlightEntity should not be null");
        assertEquals(updateFlightRequest1.getDepartureTime(), flight1.getDepartureTime(), "Departure time mismatch for flight 1");
        assertEquals(updateFlightRequest1.getArrivalTime(), flight1.getArrivalTime(), "Arrival time mismatch for flight 1");
        assertEquals(updateFlightRequest1.getPrice(), flight1.getPrice(), "Price mismatch for flight 1");

        // Validate the second FlightEntity
        FlightEntity flight2 = result.get(1);
        assertNotNull(flight2, "Second FlightEntity should not be null");
        assertEquals(updateFlightRequest2.getDepartureTime(), flight2.getDepartureTime(), "Departure time mismatch for flight 2");
        assertEquals(updateFlightRequest2.getArrivalTime(), flight2.getArrivalTime(), "Arrival time mismatch for flight 2");
        assertEquals(updateFlightRequest2.getPrice(), flight2.getPrice(), "Price mismatch for flight 2");

    }

    @Test
    void testUpdateFlightMapper_WhenFlightEntityIsUpdated() {

        // Create AirportEntities using Builder pattern
        AirportEntity departureAirportEntity = AirportEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("Departure Airport")
                .cityName("Departure City")
                .build();

        AirportEntity arrivalAirportEntity = AirportEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("Arrival Airport")
                .cityName("Arrival City")
                .build();

        // Create a flight entity and update request
        FlightEntity flightEntity = new FlightEntity();
        UpdateFlightRequest request = UpdateFlightRequest.builder()
                .fromAirportId(departureAirportEntity.getId())
                .toAirportId(arrivalAirportEntity.getId())
                .departureTime(LocalDateTime.of(2024, 1, 18, 10, 30))
                .arrivalTime(LocalDateTime.of(2024, 1, 18, 14, 45))
                .price(299.99)
                .build();

        // Call the updateFlightMapper method
        mapper.updateFlightMapper(flightEntity, request, departureAirportEntity, arrivalAirportEntity);

        // Assert: Validate that the flight entity has been updated correctly
        assertNotNull(flightEntity.getFromAirport(), "From Airport should not be null");
        assertNotNull(flightEntity.getToAirport(), "To Airport should not be null");

        // Validate mapping of fromAirport and toAirport
        assertEquals("Departure Airport", flightEntity.getFromAirport().getName(), "From Airport Name mismatch");
        assertEquals("Departure City", flightEntity.getFromAirport().getCityName(), "From Airport City mismatch");
        assertEquals("Arrival Airport", flightEntity.getToAirport().getName(), "To Airport Name mismatch");
        assertEquals("Arrival City", flightEntity.getToAirport().getCityName(), "To Airport City mismatch");

        // Validate the other properties
        assertEquals(request.getDepartureTime(), flightEntity.getDepartureTime(), "Departure Time mismatch");
        assertEquals(request.getArrivalTime(), flightEntity.getArrivalTime(), "Arrival Time mismatch");
        assertEquals(request.getPrice(), flightEntity.getPrice(), "Price mismatch");

    }

}