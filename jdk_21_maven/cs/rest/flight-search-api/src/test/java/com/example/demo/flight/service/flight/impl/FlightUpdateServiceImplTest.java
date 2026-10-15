package com.example.demo.flight.service.flight.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.AirportEntityBuilder;
import com.example.demo.builder.FlightEntityBuilder;
import com.example.demo.builder.UpdateFlightRequestBuilder;
import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.exception.FlightNotFoundException;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.FlightEntityToFlightMapper;
import com.example.demo.flight.model.mapper.flight.UpdateFlightRequestToFlightEntityMapper;
import com.example.demo.flight.repository.AirportRepository;
import com.example.demo.flight.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link FlightUpdateServiceImpl}.
 * This class verifies the correctness of the business logic in {@link FlightUpdateServiceImpl}.
 */
class FlightUpdateServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private FlightUpdateServiceImpl flightUpdateService;

    @Mock
    private FlightRepository flightRepository;

    @Mock
    private AirportRepository airportRepository;

    private final UpdateFlightRequestToFlightEntityMapper updateFlightRequestToFlightEntityMapper =
            UpdateFlightRequestToFlightEntityMapper.initialize();

    private final FlightEntityToFlightMapper flightEntityToFlightMapper =
            FlightEntityToFlightMapper.initialize();

    @Test
    void givenValidUpdateFlightRequest_whenUpdateFlightById_thenSuccess() {

        // Given
        final String flightId = UUID.randomUUID().toString();
        final String fromAirportId = UUID.randomUUID().toString();
        final String toAirportId = UUID.randomUUID().toString();

        final UpdateFlightRequest mockUpdateFlightRequest = new UpdateFlightRequestBuilder()
                .withValidFields()
                .withFromAirportId(fromAirportId)
                .withToAirportId(toAirportId)
                .build();

        final AirportEntity fromAirportEntity = new AirportEntityBuilder()
                .withId(fromAirportId)
                .withName("Origin Airport")
                .withCityName("Origin City")
                .build();

        final AirportEntity toAirportEntity = new AirportEntityBuilder()
                .withId(toAirportId)
                .withName("Destination Airport")
                .withCityName("Destination City")
                .build();

        final FlightEntity existingFlightEntity = new FlightEntityBuilder()
                .withId(flightId)
                .withValidFields()
                .build();

        final FlightEntity updatedFlightEntity = new FlightEntityBuilder()
                .withId(flightId)
                .withFromAirport(fromAirportEntity)
                .withToAirport(toAirportEntity)
                .withDepartureTime(mockUpdateFlightRequest.getDepartureTime())
                .withArrivalTime(mockUpdateFlightRequest.getArrivalTime())
                .withPrice(mockUpdateFlightRequest.getPrice())
                .build();

        final Flight expectedFlight = flightEntityToFlightMapper.map(updatedFlightEntity);

        // When
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(existingFlightEntity));
        when(airportRepository.findById(fromAirportId)).thenReturn(Optional.of(fromAirportEntity));
        when(airportRepository.findById(toAirportId)).thenReturn(Optional.of(toAirportEntity));
        when(flightRepository.save(any(FlightEntity.class))).thenReturn(updatedFlightEntity);

        // Then
        Flight result = flightUpdateService.updateFlightById(flightId, mockUpdateFlightRequest);

        // Asserts for the expected values
        assertNotNull(result);
        assertEquals(expectedFlight.getFromAirport().getName(), result.getFromAirport().getName());
        assertEquals(expectedFlight.getFromAirport().getCityName(), result.getFromAirport().getCityName());
        assertEquals(expectedFlight.getToAirport().getName(), result.getToAirport().getName());
        assertEquals(expectedFlight.getToAirport().getCityName(), result.getToAirport().getCityName());
        assertEquals(expectedFlight.getDepartureTime(), result.getDepartureTime());
        assertEquals(expectedFlight.getArrivalTime(), result.getArrivalTime());
        assertEquals(expectedFlight.getPrice(), result.getPrice());

        // Verify the mock interactions
        verify(flightRepository, times(1)).findById(flightId);
        verify(airportRepository, times(1)).findById(fromAirportId);
        verify(airportRepository, times(1)).findById(toAirportId);
        verify(flightRepository, times(1)).save(any(FlightEntity.class));

    }



    @Test
    void givenNonexistentFlightId_whenUpdateFlightById_thenThrowFlightNotFoundException() {

        // Given
        final String flightId = UUID.randomUUID().toString();

        final UpdateFlightRequest mockUpdateFlightRequest = new UpdateFlightRequestBuilder()
                .withValidFields()
                .build();

        // When
        when(flightRepository.findById(flightId)).thenReturn(Optional.empty());

        // Then
        assertThrows(FlightNotFoundException.class, () ->
                flightUpdateService.updateFlightById(flightId, mockUpdateFlightRequest));

        // Verify
        verify(flightRepository, times(1)).findById(flightId);
        verify(airportRepository, times(0)).findById(any());
        verify(flightRepository, times(0)).save(any());

    }

    @Test
    void givenNonexistentAirport_whenUpdateFlightById_thenThrowAirportNotFoundException() {

        // Given
        final String flightId = UUID.randomUUID().toString();
        final String fromAirportId = UUID.randomUUID().toString();
        final String toAirportId = UUID.randomUUID().toString();

        final UpdateFlightRequest mockUpdateFlightRequest = new UpdateFlightRequestBuilder()
                .withValidFields()
                .withFromAirportId(fromAirportId)
                .withToAirportId(toAirportId)
                .build();

        final FlightEntity existingFlightEntity = new FlightEntityBuilder()
                .withId(flightId)
                .withValidFields()
                .build();

        // When
        when(flightRepository.findById(flightId)).thenReturn(Optional.of(existingFlightEntity));
        when(airportRepository.findById(fromAirportId)).thenReturn(Optional.empty());
        when(airportRepository.findById(toAirportId)).thenReturn(Optional.of(new AirportEntity()));

        // Then
        assertThrows(AirportNotFoundException.class, () ->
                flightUpdateService.updateFlightById(flightId, mockUpdateFlightRequest));

        // Verify
        verify(flightRepository, times(1)).findById(flightId);
        verify(airportRepository, times(1)).findById(fromAirportId);
        verify(airportRepository, times(0)).findById(toAirportId);
        verify(flightRepository, times(0)).save(any());

    }


}