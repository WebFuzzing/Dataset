package com.example.demo.flight.model.mapper.airport;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.response.airport.AirportResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link AirportToAirportResponseMapperTest}.
 * This class verifies the correctness of the mapping logic from Airport objects to {@link AirportResponse}.
 */
class AirportToAirportResponseMapperTest {

    private final AirportToAirportResponseMapper mapper = AirportToAirportResponseMapper.initialize();

    @Test
    void testMapAirportNull() {

        AirportResponse result = mapper.map((Airport) null);

        assertNull(result);

    }

    @Test
    void testMapAirportCollectionNull() {

        List<AirportResponse> result = mapper.map((Collection<Airport>) null);

        assertNull(result);

    }

    @Test
    void testMapAirportListEmpty() {

        List<AirportResponse> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapAirportListWithNullElements() {

        List<Airport> tasks = Arrays.asList(new Airport(), null);

        List<AirportResponse> result = mapper.map(tasks);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleAirport() {

        Airport airport = Airport.builder()
                .id(UUID.randomUUID().toString())
                .name("Airport Name")
                .cityName("Airport City Name")
                .createdAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedAt(LocalDateTime.now())
                .updatedBy("user2")
                .build();

        AirportResponse result = mapper.map(airport);

        assertNotNull(result);
        assertEquals(airport.getId(), result.getId());
        assertEquals(airport.getName(), result.getName());

    }

    @Test
    void testMapAirportListWithValues() {

        Airport airport1 = Airport.builder()
                .id(UUID.randomUUID().toString())
                .name("Airport Name 1")
                .cityName("Airport City Name 1")
                .createdAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedAt(LocalDateTime.now())
                .updatedBy("user2")
                .build();

        Airport airport2 = Airport.builder()
                .id(UUID.randomUUID().toString())
                .name("Airport Name 2")
                .cityName("Airport City Name 2")
                .createdAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedAt(LocalDateTime.now())
                .updatedBy("user2")
                .build();

        List<Airport> tasks = Arrays.asList(airport1, airport2);
        List<AirportResponse> result = mapper.map(tasks);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(airport1.getId(), result.get(0).getId());
        assertEquals(airport2.getId(), result.get(1).getId());

    }

    @Test
    void testMapWithEdgeCaseValues() {

        Airport airport = Airport.builder()
                .id("")
                .name("")
                .build();

        AirportResponse result = mapper.map(airport);

        assertNotNull(result);
        assertEquals("", result.getId());
        assertEquals("", result.getName());

    }

}