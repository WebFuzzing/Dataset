package com.example.demo.flight.model.mapper.flight;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.response.flight.FlightResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link FlightToFlightResponseMapper}.
 * This class tests the functionality of the {@link FlightToFlightResponseMapper},
 * ensuring that {@link Flight} objects are correctly mapped to {@link FlightResponse} objects.
 * The tests verify that all fields are properly transformed and no data is lost or incorrectly
 * mapped during the conversion process.
 */
class FlightToFlightResponseMapperTest {

    private final FlightToFlightResponseMapper mapper = FlightToFlightResponseMapper.initialize();

    @Test
    void testMapFlightNull() {

        FlightResponse result = mapper.map((Flight) null);

        assertNull(result);

    }

    @Test
    void testMapFlightCollectionNull() {

        List<FlightResponse> result = mapper.map((Collection<Flight>) null);

        assertNull(result);

    }

    @Test
    void testMapFlightListEmpty() {

        List<FlightResponse> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapFlightListWithNullElements() {

        List<Flight> flights = Arrays.asList(new Flight(), null);

        List<FlightResponse> result = mapper.map(flights);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNull(result.get(1));
    }

    @Test
    void testMapSingleFlight() {

        Flight flight = Flight.builder()
                .id("flight-id-123")
                .fromAirport(Airport.builder().id("from-airport-id").name("From Airport").build())
                .toAirport(Airport.builder().id("to-airport-id").name("To Airport").build())
                .departureTime(LocalDateTime.of(2025, 1, 1, 10, 30, 0))
                .arrivalTime(LocalDateTime.of(2025, 1, 1, 14, 30, 0))
                .price(100.0)
                .build();

        FlightResponse result = mapper.map(flight);

        assertNotNull(result);
        assertEquals(flight.getId(), result.getId());
        assertEquals(flight.getFromAirport().getId(), result.getFromAirport().getId());
        assertEquals(flight.getToAirport().getId(), result.getToAirport().getId());
        assertEquals(flight.getDepartureTime(), result.getDepartureTime());
        assertEquals(flight.getArrivalTime(), result.getArrivalTime());
        assertEquals(flight.getPrice(), result.getPrice());

    }

    @Test
    void testMapFlightListWithValues() {

        Flight flight1 = Flight.builder()
                .id("flight-id-1")
                .fromAirport(Airport.builder().id("from-airport-id-1").name("From Airport 1").build())
                .toAirport(Airport.builder().id("to-airport-id-1").name("To Airport 1").build())
                .departureTime(LocalDateTime.of(2025, 1, 1, 10, 30, 0))
                .arrivalTime(LocalDateTime.of(2025, 1, 1, 14, 30, 0))
                .price(100.0)
                .build();

        Flight flight2 = Flight.builder()
                .id("flight-id-2")
                .fromAirport(Airport.builder().id("from-airport-id-2").name("From Airport 2").build())
                .toAirport(Airport.builder().id("to-airport-id-2").name("To Airport 2").build())
                .departureTime(LocalDateTime.of(2025, 2, 1, 12, 30, 0))
                .arrivalTime(LocalDateTime.of(2025, 2, 1, 16, 30, 0))
                .price(200.0)
                .build();

        List<Flight> flights = Arrays.asList(flight1, flight2);
        List<FlightResponse> result = mapper.map(flights);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(flight1.getId(), result.get(0).getId());
        assertEquals(flight2.getId(), result.get(1).getId());

    }

    @Test
    void testMapWithEdgeCaseValues() {

        Flight flight = Flight.builder()
                .id("")
                .fromAirport(Airport.builder().id("").name("").build())
                .toAirport(Airport.builder().id("").name("").build())
                .departureTime(null)
                .arrivalTime(null)
                .price(0.0)
                .build();

        FlightResponse result = mapper.map(flight);

        assertNotNull(result);
        assertEquals("", result.getId());
        assertEquals("", result.getFromAirport().getId());
        assertEquals("", result.getToAirport().getId());
        assertNull(result.getDepartureTime());
        assertNull(result.getArrivalTime());
        assertEquals(0.0, result.getPrice());

    }

}