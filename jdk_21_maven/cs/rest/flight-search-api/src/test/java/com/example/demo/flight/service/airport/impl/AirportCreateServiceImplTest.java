package com.example.demo.flight.service.airport.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.CreateAirportRequestBuilder;
import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.mapper.airport.AirportEntityToAirportMapper;
import com.example.demo.flight.model.mapper.airport.CreateAirportRequestToAirportEntityMapper;
import com.example.demo.flight.repository.AirportRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link AirportCreateServiceImpl}.
 * This class verifies the correctness of the business logic in {@link AirportCreateServiceImpl}.
 */
class AirportCreateServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private AirportCreateServiceImpl airportCreateService;

    @Mock
    private AirportRepository airportRepository;

    private final CreateAirportRequestToAirportEntityMapper createAirportRequestToAirportEntityMapper =
            CreateAirportRequestToAirportEntityMapper.initialize();

    private final AirportEntityToAirportMapper airportEntityToAirportMapper =
            AirportEntityToAirportMapper.initialize();

    @Test
    void givenValidCreateAirportRequest_whenCreateAirport_ThenReturnAirport(){

        // Given
        final CreateAirportRequest request = new CreateAirportRequestBuilder()
                .withValidFields().build();

        final AirportEntity mockAirportEntity = createAirportRequestToAirportEntityMapper.mapForSaving(request);

        final Airport mockAirport = airportEntityToAirportMapper.map(mockAirportEntity);

        // When
        when(airportRepository.existsByName(anyString())).thenReturn(false);
        when(airportRepository.save(any(AirportEntity.class))).thenReturn(mockAirportEntity);

        // Then
        Airport response = airportCreateService.createAirport(request);

        assertEquals(mockAirport.getName(),response.getName());

        // Verify
        verify(airportRepository,times(1)).save(any(AirportEntity.class));
        verify(airportRepository, times(1)).existsByName(anyString());

    }

    @Test
    void givenValidCreateAirportRequest_whenCreateAirportRequest_ThenThrowAirportNameAlreadyExistException(){

        // Given
        final CreateAirportRequest request = new CreateAirportRequestBuilder()
                .withValidFields().build();

        // When
        when(airportRepository.existsByName(request.getName())).thenReturn(true);

        // Then
        assertThrows(AirportNameAlreadyExistException.class,()-> airportCreateService.createAirport(request));

        // Verify
        verify(airportRepository,times(1)).existsByName(anyString());
        verify(airportRepository,times(0)).save(any(AirportEntity.class));

    }


}