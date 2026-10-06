package com.example.demo.flight.service.flight.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.FlightEntityBuilder;
import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.CustomPaging;
import com.example.demo.flight.exception.FlightNotFoundException;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.FlightPagingRequest;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.FlightEntityToFlightMapper;
import com.example.demo.flight.model.mapper.flight.ListFlightEntityToListFlightMapper;
import com.example.demo.flight.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link FlightReadServiceImpl}.
 * This class verifies the correctness of the business logic in {@link FlightReadServiceImpl}.
 */
class FlightReadServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private FlightReadServiceImpl flightReadService;

    @Mock
    private FlightRepository flightRepository;

    private final FlightEntityToFlightMapper flightEntityToFlightMapper =
            FlightEntityToFlightMapper.initialize();

    private final ListFlightEntityToListFlightMapper listFlightEntityToListFlightMapper =
            ListFlightEntityToListFlightMapper.initialize();

    @Test
    void givenExistFlightId_whenGetFlightById_thenReturnFlight() {

        // Given
        final String mockId = UUID.randomUUID().toString();

        final FlightEntity flightEntity = new FlightEntityBuilder()
                .withId(mockId)
                .withValidFields()
                .build();

        final Flight mockFlight = flightEntityToFlightMapper.map(flightEntity);

        // When
        when(flightRepository.findById(mockId)).thenReturn(Optional.of(flightEntity));

        // Then
        final Flight expected = flightReadService.getFlightById(mockId);

        assertNotNull(expected);
        assertEquals(mockFlight.getId(), expected.getId());
        assertEquals(mockFlight.getToAirport().getName(), expected.getToAirport().getName());
        assertEquals(mockFlight.getFromAirport().getName(), expected.getFromAirport().getName());
        assertEquals(mockFlight.getArrivalTime(), expected.getArrivalTime());
        assertEquals(mockFlight.getDepartureTime(), expected.getDepartureTime());
        assertEquals(mockFlight.getPrice(), expected.getPrice());

        // Verify
        verify(flightRepository, times(1)).findById(mockId);
    }

    @Test
    void givenNonExistFlightId_whenGetFlightById_thenThrowFlightNotFoundException() {

        // Given
        final String mockId = UUID.randomUUID().toString();

        // When
        when(flightRepository.findById(mockId)).thenReturn(Optional.empty());

        // Then
        assertThrows(FlightNotFoundException.class,
                () -> flightReadService.getFlightById(mockId));

        // Verify
        verify(flightRepository, times(1)).findById(mockId);

    }

    @Test
    void givenPagingRequest_WhenFlightsAreFound_ThenReturnCustomPageFlightList() {

        // Given
        final FlightPagingRequest pagingRequest = FlightPagingRequest.builder()
                .pagination(
                        CustomPaging.builder()
                                .pageSize(1)
                                .pageNumber(1)
                                .build()
                ).build();

        final FlightEntity flightEntity = new FlightEntityBuilder().withValidFields().build();

        Page<FlightEntity> flightEntityPage = new PageImpl<>(Collections.singletonList(flightEntity));

        List<Flight> flights = listFlightEntityToListFlightMapper.toFlightList(flightEntityPage.getContent());

        CustomPage<Flight> expected = CustomPage.of(flights, flightEntityPage);

        // When
        when(flightRepository.findAll(any(Pageable.class))).thenReturn(flightEntityPage);

        // Then
        CustomPage<Flight> result = flightReadService.getAllFlights(pagingRequest);

        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
        assertEquals(expected.getPageNumber(), result.getPageNumber());
        assertEquals(expected.getContent().get(0).getId(), result.getContent().get(0).getId());
        assertEquals(expected.getTotalPageCount(), result.getTotalPageCount());
        assertEquals(expected.getTotalElementCount(), result.getTotalElementCount());

        // Verify
        verify(flightRepository, times(1)).findAll(any(Pageable.class));

    }

    @Test
    void givenPagingRequest_WhenNoFlightsAreFound_ThenThrowFlightNotFoundException() {

        // Given
        final FlightPagingRequest pagingRequest = FlightPagingRequest.builder()
                .pagination(
                        CustomPaging.builder()
                                .pageSize(1)
                                .pageNumber(1)
                                .build()
                ).build();

        Page<FlightEntity> flightEntityPage = new PageImpl<>(Collections.emptyList());

        // When
        when(flightRepository.findAll(any(Pageable.class))).thenReturn(flightEntityPage);

        // Then
        assertThrows(FlightNotFoundException.class, () -> flightReadService.getAllFlights(pagingRequest));

        // Verify
        verify(flightRepository, times(1)).findAll(any(Pageable.class));

    }


}