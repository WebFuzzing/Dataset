package com.example.demo.flight.model.mapper.airport;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.entity.AirportEntity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for verifying the behavior of the {@link ListAirportEntityToListAirportMapper}.
 * It tests the {@link ListAirportEntityToListAirportMapper#toAirportList} method to ensure that it
 * returns {@code null} when the input list of {@link AirportEntity} objects is {@code null}.
 */
class ListAirportEntityToListAirportMapperTest {

    private final ListAirportEntityToListAirportMapper mapper =
            ListAirportEntityToListAirportMapper.initialize();

    @Test
    void testToAirportList_WhenAirportEntitiesIsNull() {
        // Test case where airportEntities is null
        List<AirportEntity> airportEntities = null;

        List<Airport> result = mapper.toAirportList(airportEntities);

        // Assert that the result is null when airportEntities is null
        assertNull(result);
    }

}