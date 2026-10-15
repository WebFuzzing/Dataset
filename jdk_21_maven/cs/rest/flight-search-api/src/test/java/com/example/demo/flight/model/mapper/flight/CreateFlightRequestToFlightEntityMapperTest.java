package com.example.demo.flight.model.mapper.flight;

import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.entity.FlightEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link CreateFlightRequestToFlightEntityMapper}.
 * This class tests the functionality of the {@link CreateFlightRequestToFlightEntityMapper},
 * ensuring that {@link CreateFlightRequest} objects are correctly mapped to {@link FlightEntity} objects.
 * The tests verify that all fields are mapped properly and that no data is lost or incorrectly transformed during the mapping process.
 */
class CreateFlightRequestToFlightEntityMapperTest {

    private final CreateFlightRequestToFlightEntityMapperImpl mapper = new CreateFlightRequestToFlightEntityMapperImpl();

    @Test
    void testMapCreateFlightRequestNull() {

        FlightEntity result = mapper.map((CreateFlightRequest) null);

        assertNull(result);

    }

    @Test
    void testMapCreateFlightRequestListNull() {

        List<FlightEntity> result = mapper.map((Collection<CreateFlightRequest>) null);

        assertNull(result);

    }

    @Test
    void testMapCreateFlightRequestListEmpty() {

        List<FlightEntity> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapCreateFlightRequestListWithNullElements() {

        CreateFlightRequest request1 = CreateFlightRequest.builder()
                .departureTime(LocalDateTime.now())
                .arrivalTime(LocalDateTime.now().plusHours(2))
                .price(200.0)
                .build();
        CreateFlightRequest request2 = null;

        List<CreateFlightRequest> requests = Arrays.asList(request1, request2);

        List<FlightEntity> result = mapper.map(requests);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(0));
        assertNull(result.get(1));

    }

    @Test
    void testMapCreateFlightRequestForSaving() {

        CreateFlightRequest request = CreateFlightRequest.builder()
                .departureTime(LocalDateTime.now())
                .arrivalTime(LocalDateTime.now().plusHours(2))
                .price(200.0)
                .build();

        FlightEntity result = mapper.map(request);

        assertNotNull(result);
        assertEquals(request.getDepartureTime(), result.getDepartureTime());
        assertEquals(request.getArrivalTime(), result.getArrivalTime());
        assertEquals(request.getPrice(), result.getPrice());

    }

    @Test
    void testMapCreateFlightRequestListWithValues() {

        CreateFlightRequest request1 = CreateFlightRequest.builder()
                .departureTime(LocalDateTime.now())
                .arrivalTime(LocalDateTime.now().plusHours(2))
                .price(200.0)
                .build();

        CreateFlightRequest request2 = CreateFlightRequest.builder()
                .departureTime(LocalDateTime.now())
                .arrivalTime(LocalDateTime.now().plusHours(3))
                .price(300.0)
                .build();

        List<CreateFlightRequest> requests = Arrays.asList(request1, request2);

        List<FlightEntity> result = mapper.map(requests);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(request1.getPrice(), result.get(0).getPrice());
        assertEquals(request2.getPrice(), result.get(1).getPrice());

    }

    @Test
    void testMapCreateFlightRequestForSavingWithNullValues() {

        CreateFlightRequest request = CreateFlightRequest.builder()
                .departureTime(null)
                .arrivalTime(null)
                .price(null)
                .build();

        FlightEntity result = mapper.map(request);

        assertNotNull(result);
        assertNull(result.getDepartureTime());
        assertNull(result.getArrivalTime());
        assertNull(result.getPrice());

    }

    @Test
    void testMapCreateFlightRequestListWithEdgeCaseValues() {

        CreateFlightRequest request = CreateFlightRequest.builder()
                .departureTime(null)
                .arrivalTime(null)
                .price(0.0)
                .build();

        FlightEntity result = mapper.map(request);

        assertNotNull(result);
        assertNull(result.getDepartureTime());
        assertNull(result.getArrivalTime());
        assertEquals(0.0, result.getPrice());

    }

    @Test
    void testMapCreateFlightRequestForSavingWithNullFields() {

        CreateFlightRequest request = CreateFlightRequest.builder()
                .departureTime(null)
                .arrivalTime(null)
                .price(0.0)
                .build();

        FlightEntity result = mapper.map(request);

        assertNotNull(result);
        assertNull(result.getDepartureTime());
        assertNull(result.getArrivalTime());
        assertEquals(0.0, result.getPrice());


    }

}