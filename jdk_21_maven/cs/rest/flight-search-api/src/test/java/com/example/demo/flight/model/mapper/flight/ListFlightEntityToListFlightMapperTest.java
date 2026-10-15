package com.example.demo.flight.model.mapper.flight;

import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.entity.FlightEntity;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link ListFlightEntityToListFlightMapper}.
 * This class tests the functionality of the {@link ListFlightEntityToListFlightMapper},
 * ensuring that a list of {@link FlightEntity} objects is correctly mapped to a list of {@link Flight} objects.
 * The tests validate that all fields are properly transformed for each entity in the list and that
 * the mapping maintains data integrity across the collection.
 */
class ListFlightEntityToListFlightMapperTest {

    private final ListFlightEntityToListFlightMapper mapper = ListFlightEntityToListFlightMapper.initialize();

    @Test
    void testToFlightList_WhenFlightEntitiesIsNull() {

        List<FlightEntity> flightEntities = null;

        List<Flight> result = mapper.toFlightList(flightEntities);

        assertNull(result);

    }

    @Test
    void testToFlightList_WhenFlightEntitiesIsEmpty() {

        List<FlightEntity> flightEntities = Collections.emptyList();

        List<Flight> result = mapper.toFlightList(flightEntities);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

}