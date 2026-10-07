package com.example.demo.flight.service.airport.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.AirportEntityBuilder;
import com.example.demo.builder.UpdateAirportRequestBuilder;
import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.mapper.airport.AirportEntityToAirportMapper;
import com.example.demo.flight.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link AirportUpdateServiceImpl}.
 * This class verifies the correctness of the business logic in {@link AirportUpdateServiceImpl}.
 */
class AirportUpdateServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private AirportUpdateServiceImpl airportUpdateService;

    @Mock
    private AirportRepository airportRepository;

    private final AirportEntityToAirportMapper airportEntityToAirportMapper =
            AirportEntityToAirportMapper.initialize();

    @Test
    void givenUpdateAirportRequest_whenUpdateAirportById_thenSuccess() {

        // Given
        final String mockId = UUID.randomUUID().toString();

        final UpdateAirportRequest mockUpdateAirportRequest = new UpdateAirportRequestBuilder()
                .withValidFields()
                .build();

        final AirportEntity existingAirportEntity = new AirportEntityBuilder()
                .withId(mockId)
                .withValidFields();

        final AirportEntity updatedAirportEntity = new AirportEntityBuilder()
                .withId(mockId)
                .withName(mockUpdateAirportRequest.getName())
                .withCityName(mockUpdateAirportRequest.getCityName())
                .build();

        final Airport expected = airportEntityToAirportMapper.map(updatedAirportEntity);

        // When
        when(airportRepository.findById(mockId)).thenReturn(Optional.of(existingAirportEntity));
        when(airportRepository.save(existingAirportEntity)).thenReturn(updatedAirportEntity);

        Airport result = airportUpdateService.updateAirportById(mockId, mockUpdateAirportRequest);

        // Then
        assertNotNull(result);
        assertEquals(expected.getName(), result.getName());
        assertEquals(expected.getCityName(), result.getCityName());

        // Verify
        verify(airportRepository,times(1)).findById(mockId);
        verify(airportRepository,times(1)).save(existingAirportEntity);

    }

    @Test
    void givenNonexistentAirportId_whenUpdateAirportById_thenThrowAirportNotFoundException() {

        // Given
        final String airportId = UUID.randomUUID().toString();

        final UpdateAirportRequest updateRequest = new UpdateAirportRequestBuilder()
                .withValidFields()
                .build();

        // When
        when(airportRepository.findById(airportId)).thenReturn(Optional.empty());

        // Then
        assertThrows(AirportNotFoundException.class, () ->
                airportUpdateService.updateAirportById(airportId, updateRequest));

        // Verify
        verify(airportRepository, times(1)).findById(airportId);

    }

    @Test
    void givenDuplicateAirportName_whenUpdateAirportById_thenThrowIllegalArgumentException() {

        // Given
        final String airportId = UUID.randomUUID().toString();
        final UpdateAirportRequest updateRequest = new UpdateAirportRequestBuilder()
                .withName("Duplicate Name")
                .withCityName("New Location")
                .build();

        // When
        when(airportRepository.existsByName(updateRequest.getName())).thenReturn(true);

        // Then
        AirportNameAlreadyExistException exception = assertThrows(AirportNameAlreadyExistException.class, () ->
                airportUpdateService.updateAirportById(airportId, updateRequest));

        assertEquals("Airport with this name already exist\n" +
                " With given airport name = Duplicate Name", exception.getMessage());

        // Verify
        verify(airportRepository,times(0)).findById(airportId);
        verify(airportRepository,times(1)).existsByName(updateRequest.getName());

    }

}