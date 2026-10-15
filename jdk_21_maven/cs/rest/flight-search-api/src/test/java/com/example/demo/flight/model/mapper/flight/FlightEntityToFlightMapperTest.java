package com.example.demo.flight.model.mapper.flight;

import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link FlightEntityToFlightMapper}.
 * This class ensures that {@link FlightEntity} objects are correctly mapped to their domain-level representation.
 */
class FlightEntityToFlightMapperTest {

    private final FlightEntityToFlightMapper mapper = FlightEntityToFlightMapper.initialize();

    @Test
    void testMapFlightEntityNull() {

        // Test null single FlightEntity
        Flight result = mapper.map((FlightEntity) null);
        assertNull(result);

    }

    @Test
    void testMapFlightEntityCollectionNull() {

        // Test null collection of FlightEntity
        List<Flight> result = mapper.map((Collection<FlightEntity>) null);
        assertNull(result);

    }

    @Test
    void testMapFlightEntityListEmpty() {

        // Test empty list of FlightEntity
        List<Flight> result = mapper.map(Collections.emptyList());
        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapFlightEntityListWithNullElements() {

        // Test list with null FlightEntity elements
        FlightEntity flightEntity1 = new FlightEntity();
        List<FlightEntity> flightEntities = Arrays.asList(flightEntity1, null);
        List<Flight> result = mapper.map(flightEntities);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleFlightEntity() {

        // Test mapping a single FlightEntity to Flight
        FlightEntity flightEntity = FlightEntity.builder()
                .id(UUID.randomUUID().toString())
                .fromAirport(AirportEntity.builder().name("From Airport").cityName("City A").build())
                .toAirport(AirportEntity.builder().name("To Airport").cityName("City B").build())
                .departureTime(LocalDateTime.now().plusHours(2))
                .arrivalTime(LocalDateTime.now().plusHours(3))
                .price(150.00)
                .build();

        Flight result = mapper.map(flightEntity);

        assertNotNull(result);
        assertEquals(flightEntity.getId(), result.getId());
        assertNotNull(result.getFromAirport());
        assertNotNull(result.getToAirport());
        assertEquals(flightEntity.getFromAirport().getName(), result.getFromAirport().getName());
        assertEquals(flightEntity.getToAirport().getName(), result.getToAirport().getName());
        assertEquals(flightEntity.getDepartureTime(), result.getDepartureTime());
        assertEquals(flightEntity.getArrivalTime(), result.getArrivalTime());
        assertEquals(flightEntity.getPrice(), result.getPrice());

    }

    @Test
    void testMapFlightEntityListWithValues() {

        // Test mapping a list of FlightEntity to Flight
        FlightEntity flightEntity1 = FlightEntity.builder()
                .id(UUID.randomUUID().toString())
                .fromAirport(AirportEntity.builder().name("From Airport 1").cityName("City A1").build())
                .toAirport(AirportEntity.builder().name("To Airport 1").cityName("City B1").build())
                .departureTime(LocalDateTime.now().plusHours(2))
                .arrivalTime(LocalDateTime.now().plusHours(3))
                .price(120.00)
                .build();

        FlightEntity flightEntity2 = FlightEntity.builder()
                .id(UUID.randomUUID().toString())
                .fromAirport(AirportEntity.builder().name("From Airport 2").cityName("City A2").build())
                .toAirport(AirportEntity.builder().name("To Airport 2").cityName("City B2").build())
                .departureTime(LocalDateTime.now().plusHours(4))
                .arrivalTime(LocalDateTime.now().plusHours(5))
                .price(200.00)
                .build();

        List<FlightEntity> flightEntities = Arrays.asList(flightEntity1, flightEntity2);
        List<Flight> result = mapper.map(flightEntities);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(flightEntity1.getId(), result.get(0).getId());
        assertEquals(flightEntity2.getId(), result.get(1).getId());
        assertEquals(flightEntity1.getFromAirport().getName(), result.get(0).getFromAirport().getName());
        assertEquals(flightEntity2.getFromAirport().getName(), result.get(1).getFromAirport().getName());

    }

    @Test
    void testMapWithEdgeCaseValues() {

        // Test edge case with empty or null values
        FlightEntity flightEntity = FlightEntity.builder()
                .fromAirport(AirportEntity.builder().name("").cityName(null).build())
                .toAirport(AirportEntity.builder().name(null).cityName("").build())
                .departureTime(null)
                .arrivalTime(null)
                .price(null)
                .build();

        Flight result = mapper.map(flightEntity);

        assertNotNull(result);
        assertEquals("", result.getFromAirport().getName());
        assertNull(result.getFromAirport().getCityName());
        assertNull(result.getToAirport().getName());
        assertEquals("", result.getToAirport().getCityName());
        assertNull(result.getDepartureTime());
        assertNull(result.getArrivalTime());
        assertNull(result.getPrice());

    }

}