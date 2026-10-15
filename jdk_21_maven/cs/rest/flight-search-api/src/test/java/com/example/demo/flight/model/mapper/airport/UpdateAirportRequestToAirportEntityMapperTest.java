package com.example.demo.flight.model.mapper.airport;

import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link UpdateAirportRequestToAirportEntityMapper}.
 * This class tests the functionality of mapping between {@link UpdateAirportRequest}
 * and {@link AirportEntity} using the {@link UpdateAirportRequestToAirportEntityMapper}.
 */
class UpdateAirportRequestToAirportEntityMapperTest {

    private final UpdateAirportRequestToAirportEntityMapper mapper = UpdateAirportRequestToAirportEntityMapper.initialize();

    @Test
    void testMap_WhenUpdateAirportRequestIsNull() {

        AirportEntity result = mapper.map((UpdateAirportRequest) null);

        assertNull(result);

    }

    @Test
    void testMap_WhenUpdateAirportRequestIsValid() {

        UpdateAirportRequest updateAirportRequest = new UpdateAirportRequest("Airport Name",
                "City Name");

        AirportEntity result = mapper.map(updateAirportRequest);

        assertNotNull(result);
        assertEquals("Airport Name", result.getName());
        assertEquals("City Name", result.getCityName());

    }

    @Test
    void testMap_WhenUpdateAirportRequestIsEmpty() {

        UpdateAirportRequest updateAirportRequest = new UpdateAirportRequest("", "");

        AirportEntity result = mapper.map(updateAirportRequest);

        assertNotNull(result);
        assertEquals("", result.getName());
        assertEquals("", result.getCityName());

    }

    @Test
    void testMap_WhenUpdateAirportRequestHasNullNameAndCity() {

        UpdateAirportRequest updateAirportRequest = new UpdateAirportRequest(null, null);

        AirportEntity result = mapper.map(updateAirportRequest);

        assertNotNull(result);
        assertNull(result.getName());
        assertNull(result.getCityName());

    }

    @Test
    void testMap_WhenUpdateAirportRequestCollectionIsNull() {

        List<UpdateAirportRequest> updateAirportRequests = null;

        List<AirportEntity> result = mapper.map(updateAirportRequests);

        assertNull(result);

    }

    @Test
    void testMap_WhenUpdateAirportRequestCollectionIsEmpty() {

        List<UpdateAirportRequest> updateAirportRequests = Collections.emptyList();

        List<AirportEntity> result = mapper.map(updateAirportRequests);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMap_WhenUpdateAirportRequestCollectionHasValidElements() {

        UpdateAirportRequest updateAirportRequest1 = new UpdateAirportRequest("Airport 1", "City 1");
        UpdateAirportRequest updateAirportRequest2 = new UpdateAirportRequest("Airport 2", "City 2");

        List<UpdateAirportRequest> updateAirportRequests = Arrays.asList(updateAirportRequest1, updateAirportRequest2);

        List<AirportEntity> result = mapper.map(updateAirportRequests);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Airport 1", result.get(0).getName());
        assertEquals("City 1", result.get(0).getCityName());
        assertEquals("Airport 2", result.get(1).getName());
        assertEquals("City 2", result.get(1).getCityName());

    }

    @Test
    void testUpdateAirportMapper_WhenAirportEntityIsNull() {

        AirportEntity airportEntity = new AirportEntity();
        UpdateAirportRequest updateAirportRequest = new UpdateAirportRequest("Updated Airport",
                "Updated City");

        mapper.updateAirportMapper(airportEntity, updateAirportRequest);

        assertEquals("Updated Airport", airportEntity.getName());
        assertEquals("Updated City", airportEntity.getCityName());

    }

}