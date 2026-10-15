package com.example.demo.flight.model.mapper.airport;

import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.entity.AirportEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link AirportEntityToAirportMapper}.
 * This class ensures that {@link AirportEntity} objects are correctly mapped to their domain-level representation.
 */
class AirportEntityToAirportMapperTest {

    private final AirportEntityToAirportMapper mapper = AirportEntityToAirportMapper.initialize();

    @Test
    void testMapAirportEntityNull() {
        // Test null single TaskEntity
        Airport result = mapper.map((AirportEntity) null);
        assertNull(result);
    }

    @Test
    void testMapAirportEntityCollectionNull() {

        List<Airport> result = mapper.map((Collection<AirportEntity>) null);

        assertNull(result);

    }

    @Test
    void testMapAirportEntityListEmpty() {

        List<Airport> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapAirportEntityListWithNullElements() {

        List<AirportEntity> airportEntities = Arrays.asList(new AirportEntity(), null);
        List<Airport> result = mapper.map(airportEntities);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleAirportEntity() {

        AirportEntity airportEntity = AirportEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("Airport Name")
                .cityName("Airport City Name")
                .createdAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedAt(LocalDateTime.now())
                .updatedBy("user2")
                .build();

        Airport result = mapper.map(airportEntity);

        assertNotNull(result);
        assertEquals(airportEntity.getId(), result.getId());
        assertEquals(airportEntity.getName(), result.getName());
        assertEquals(airportEntity.getCreatedAt(), result.getCreatedAt());
        assertEquals(airportEntity.getCreatedBy(), result.getCreatedBy());
        assertEquals(airportEntity.getUpdatedAt(), result.getUpdatedAt());
        assertEquals(airportEntity.getUpdatedBy(), result.getUpdatedBy());

    }

    @Test
    void testMapAirportEntityListWithValues() {

        AirportEntity airportEntity1 = AirportEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("Airport Name 1")
                .cityName("Airport City Name 1")
                .createdAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedAt(LocalDateTime.now())
                .updatedBy("user2")
                .build();

        AirportEntity airportEntity2 = AirportEntity.builder()
                .id(UUID.randomUUID().toString())
                .name("Airport Name 2")
                .cityName("Airport City Name 2")
                .createdAt(LocalDateTime.now())
                .createdBy("user1")
                .updatedAt(LocalDateTime.now())
                .updatedBy("user2")
                .build();

        List<AirportEntity> taskEntities = Arrays.asList(airportEntity1, airportEntity2);
        List<Airport> result = mapper.map(taskEntities);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(airportEntity1.getId(), result.get(0).getId());
        assertEquals(airportEntity2.getId(), result.get(1).getId());

    }

    @Test
    void testMapWithEdgeCaseValues() {

        AirportEntity airportEntity = AirportEntity.builder()
                .name("")
                .createdAt(null)
                .createdBy(null)
                .updatedAt(null)
                .updatedBy(null)
                .build();

        Airport result = mapper.map(airportEntity);

        assertNotNull(result);
        assertEquals("", result.getName());
        assertNull(result.getCreatedAt());
        assertNull(result.getCreatedBy());
        assertNull(result.getUpdatedAt());
        assertNull(result.getUpdatedBy());

    }

}