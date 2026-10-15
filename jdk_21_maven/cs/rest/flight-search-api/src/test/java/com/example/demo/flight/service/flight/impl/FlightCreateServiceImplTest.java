package com.example.demo.flight.service.flight.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.CreateFlightRequestBuilder;
import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.CreateFlightRequestToFlightEntityMapper;
import com.example.demo.flight.model.mapper.flight.FlightEntityToFlightMapper;
import com.example.demo.flight.repository.AirportRepository;
import com.example.demo.flight.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link FlightCreateServiceImpl}.
 * This class verifies the correctness of the business logic in {@link FlightCreateServiceImpl}.
 */
class FlightCreateServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private FlightCreateServiceImpl flightCreateService;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportRepository airportRepository;

    private final CreateFlightRequestToFlightEntityMapper createFlightRequestToFlightEntityMapper =
            CreateFlightRequestToFlightEntityMapper.initialize();

    private final FlightEntityToFlightMapper flightEntityToFlightMapper =
            FlightEntityToFlightMapper.initialize();

    @Test
    void givenValidCreateFlightRequest_whenCreateFlight_ThenReturnFlight() {

        // Given
        final CreateFlightRequest request = new CreateFlightRequestBuilder()
                .withValidFields().build();

        final AirportEntity departureAirport = new AirportEntity("AIRPORT1", "Airport Name", "City Name");
        final AirportEntity arrivalAirport = new AirportEntity("AIRPORT2", "Destination Name", "City Name");

        final FlightEntity mockFlightEntity = createFlightRequestToFlightEntityMapper
                .mapForSaving(request, departureAirport, arrivalAirport);

        final Flight mockFlight = flightEntityToFlightMapper.map(mockFlightEntity);

        // When
        when(airportRepository.findById(request.getFromAirportId())).thenReturn(Optional.of(departureAirport));
        when(airportRepository.findById(request.getToAirportId())).thenReturn(Optional.of(arrivalAirport));
        when(flightRepository.save(any(FlightEntity.class))).thenReturn(mockFlightEntity);

        // Then
        Flight response = flightCreateService.createFlight(request);

        assertEquals(mockFlight.getId(), response.getId());
        assertEquals(mockFlight.getFromAirport().getId(), response.getFromAirport().getId());
        assertEquals(mockFlight.getToAirport().getId(), response.getToAirport().getId());
        assertEquals(mockFlight.getDepartureTime(), response.getDepartureTime());
        assertEquals(mockFlight.getArrivalTime(), response.getArrivalTime());
        assertEquals(mockFlight.getPrice(), response.getPrice());

        // Verify
        verify(airportRepository, times(1)).findById(request.getFromAirportId());
        verify(airportRepository, times(1)).findById(request.getToAirportId());
        verify(flightRepository, times(1)).save(any(FlightEntity.class));

    }

    @Test
    void givenNonExistentDepartureAirport_whenCreateFlight_ThenThrowAirportNotFoundException() {

        // Given
        final CreateFlightRequest request = new CreateFlightRequestBuilder()
                .withValidFields().build();

        // When
        when(airportRepository.findById(request.getFromAirportId())).thenReturn(Optional.empty());

        // Then
        AirportNotFoundException exception = assertThrows(AirportNotFoundException.class, () ->
                flightCreateService.createFlight(request)
        );
        assertEquals("Airport not found!\n" +
                " Departure airport not found with id AIRPORT1", exception.getMessage());

        // Verify
        verify(airportRepository, times(1)).findById(request.getFromAirportId());
        verify(airportRepository, times(0)).findById(request.getToAirportId());
    }

    @Test
    void givenNonExistentArrivalAirport_whenCreateFlight_ThenThrowAirportNotFoundException() {

        // Given
        final CreateFlightRequest request = new CreateFlightRequestBuilder()
                .withValidFields().build();

        final AirportEntity departureAirport = new AirportEntity("AIRPORT1", "Airport Name", "City Name");

        // When
        when(airportRepository.findById(request.getFromAirportId())).thenReturn(Optional.of(departureAirport));
        when(airportRepository.findById(request.getToAirportId())).thenReturn(Optional.empty());

        // Then
        AirportNotFoundException exception = assertThrows(AirportNotFoundException.class, () ->
                flightCreateService.createFlight(request)
        );
        assertEquals("Airport not found!\n" +
                " Arrival airport not found with id AIRPORT2", exception.getMessage());

        // Verify
        verify(airportRepository, times(1)).findById(request.getFromAirportId());
        verify(airportRepository, times(1)).findById(request.getToAirportId());
    }


}