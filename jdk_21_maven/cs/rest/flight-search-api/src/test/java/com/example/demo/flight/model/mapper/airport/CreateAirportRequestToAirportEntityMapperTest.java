package com.example.demo.flight.model.mapper.airport;

import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link CreateAirportRequestToAirportEntityMapperTest}.
 * This class verifies the correctness of the mapping logic from {@link CreateAirportRequest}
 * to {@link AirportEntity}.
 */
class CreateAirportRequestToAirportEntityMapperTest {

    private final CreateAirportRequestToAirportEntityMapper mapper = CreateAirportRequestToAirportEntityMapper.initialize();

    @Test
    void testMapCreateAirportRequestNull() {

        AirportEntity result = mapper.map((CreateAirportRequest) null);

        assertNull(result);

    }

    @Test
    void testMapCreateAirportRequestCollectionNull() {

        List<AirportEntity> result = mapper.map((Collection<CreateAirportRequest>) null);

        assertNull(result);

    }

    @Test
    void testMapCreateAirportRequestListEmpty() {

        List<AirportEntity> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapCreateAirportRequestListWithNullElements() {

        List<CreateAirportRequest> requests =
                Arrays.asList(new CreateAirportRequest("airportName","airportCityName"),null);

        List<AirportEntity> result = mapper.map(requests);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleCreateAirportRequest() {

        CreateAirportRequest request = new CreateAirportRequest("airportName","airportCityName");

        AirportEntity result = mapper.map(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());

    }

    @Test
    void testMapCreateAirportRequestListWithValues() {

        CreateAirportRequest request1 = new CreateAirportRequest("airportName1","airportCityName1");
        CreateAirportRequest request2 = new CreateAirportRequest("airportName2","airportCityName2");

        List<CreateAirportRequest> requests = Arrays.asList(request1, request2);

        List<AirportEntity> result = mapper.map(requests);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(request1.getName(), result.get(0).getName());
        assertEquals(request2.getName(), result.get(1).getName());

    }

    @Test
    void testMapForSaving() {

        CreateAirportRequest request = new CreateAirportRequest("airportName","airportCityName");

        AirportEntity result = mapper.mapForSaving(request);

        assertNotNull(result);
        assertEquals(request.getName(), result.getName());

    }

    @Test
    void testMapWithEdgeCaseValues() {

        CreateAirportRequest request = new CreateAirportRequest("","");

        AirportEntity result = mapper.map(request);

        assertNotNull(result);
        assertEquals("", result.getName());

    }

}