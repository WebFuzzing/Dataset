package com.example.demo.flight.service.flight.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.AirportEntityBuilder;
import com.example.demo.builder.FlightEntityBuilder;
import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.CustomPaging;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.SearchFlightRequest;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.ListFlightEntityToListFlightMapper;
import com.example.demo.flight.repository.SearchFlightRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for {@link SearchFlightServiceImpl}.
 * This class verifies the correctness of the business logic in {@link SearchFlightServiceImpl}.
 */
class SearchFlightServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private SearchFlightServiceImpl searchFlightService;

    @Mock
    private SearchFlightRepository searchFlightRepository;

    private final ListFlightEntityToListFlightMapper listFlightEntityToListFlightMapper =
            ListFlightEntityToListFlightMapper.initialize();


    @Test
    void givenSearchFlightRequest_whenOneWayFlightsFound_thenReturnFlights() {

        // Given
        final CustomPaging pagination = CustomPaging.builder()
                .pageNumber(1)
                .pageSize(10)
                .build();

        final SearchFlightRequest request = SearchFlightRequest.builder()
                .fromAirportId(UUID.randomUUID().toString())
                .toAirportId(UUID.randomUUID().toString())
                .departureTime(LocalDateTime.of(2025, 1, 19, 10, 0))
                .pagination(pagination)
                .build();

        final LocalDateTime departureStart = LocalDateTime.of(2025, 1, 19, 0, 0);
        final LocalDateTime departureEnd = departureStart.plusDays(1).minusNanos(1);

        final FlightEntity flightEntity = new FlightEntityBuilder()
                .withValidFields()
                .withFromAirport(new AirportEntityBuilder()
                        .withId(request.getFromAirportId())
                        .withValidFields())
                .withToAirport(new AirportEntityBuilder()
                        .withId(request.getToAirportId())
                        .withValidFields())
                .withDepartureTime(LocalDateTime.of(2025, 1, 19, 10, 0))
                .build();

        final Page<FlightEntity> departureFlightEntities = new PageImpl<>(List.of(flightEntity));

        final List<Flight> flightList = listFlightEntityToListFlightMapper.toFlightList(departureFlightEntities.getContent());

        // When
        when(searchFlightRepository.findFlights(
                request.getFromAirportId(), request.getToAirportId(), departureStart, departureEnd, request.toPageable())).thenReturn(departureFlightEntities);

        // Then
        CustomPage<Flight> result = searchFlightService.searchFlights(request);

        assertEquals(flightList.size(), result.getContent().size());

        assertEquals(flightList.get(0).getFromAirport().getId(), result.getContent().get(0).getFromAirport().getId());
        assertEquals(flightList.get(0).getFromAirport().getName(), result.getContent().get(0).getFromAirport().getName());
        assertEquals(flightList.get(0).getFromAirport().getCityName(), result.getContent().get(0).getFromAirport().getCityName());
        assertEquals(flightList.get(0).getToAirport().getId(), result.getContent().get(0).getToAirport().getId());
        assertEquals(flightList.get(0).getToAirport().getName(), result.getContent().get(0).getToAirport().getName());
        assertEquals(flightList.get(0).getToAirport().getCityName(), result.getContent().get(0).getToAirport().getCityName());
        assertEquals(flightList.get(0).getPrice(), result.getContent().get(0).getPrice());

        // Verify
        verify(searchFlightRepository).findFlights(request.getFromAirportId(), request.getToAirportId(),
                departureStart, departureEnd, request.toPageable());

    }


    @Test
    void givenSearchFlightRequest_whenRoundTripFlightsFound_thenReturnFlights() {

        // Given
        final CustomPaging pagination = CustomPaging.builder()
                .pageNumber(1)
                .pageSize(10)
                .build();

        final SearchFlightRequest request = SearchFlightRequest.builder()
                .fromAirportId(UUID.randomUUID().toString())
                .toAirportId(UUID.randomUUID().toString())
                .departureTime(LocalDateTime.of(2025, 1, 19, 10, 0))
                .arrivalTime(LocalDateTime.of(2025, 1, 20, 10, 0))
                .pagination(pagination)
                .build();

        final LocalDateTime departureStart = LocalDateTime.of(2025, 1, 19, 0, 0);
        final LocalDateTime departureEnd = departureStart.plusDays(1).minusNanos(1);

        final LocalDateTime returnStart = LocalDateTime.of(2025, 1, 20, 0, 0);
        final LocalDateTime returnEnd = returnStart.plusDays(1).minusNanos(1);

        final FlightEntity departureFlightEntity = new FlightEntityBuilder()
                .withValidFields()
                .withFromAirport(new AirportEntityBuilder()
                        .withId(request.getFromAirportId())
                        .withValidFields())
                .withToAirport(new AirportEntityBuilder()
                        .withId(request.getToAirportId())
                        .withValidFields())
                .withDepartureTime(LocalDateTime.of(2025, 1, 19, 10, 0))
                .build();

        final FlightEntity returnFlightEntity = new FlightEntityBuilder()
                .withValidFields()
                .withFromAirport(new AirportEntityBuilder()
                        .withId(request.getToAirportId())
                        .withValidFields())
                .withToAirport(new AirportEntityBuilder()
                        .withId(request.getFromAirportId())
                        .withValidFields())
                .withDepartureTime(LocalDateTime.of(2025, 1, 20, 10, 0))
                .build();

        final Page<FlightEntity> departureFlightEntities = new PageImpl<>(List.of(departureFlightEntity));
        final Page<FlightEntity> returnFlightEntities = new PageImpl<>(List.of(returnFlightEntity));

        final List<Flight> departureFlights = listFlightEntityToListFlightMapper.toFlightList(departureFlightEntities.getContent());
        final List<Flight> returnFlights = listFlightEntityToListFlightMapper.toFlightList(returnFlightEntities.getContent());

        // When
        when(searchFlightRepository.findFlights(
                request.getFromAirportId(), request.getToAirportId(), departureStart, departureEnd, request.toPageable())).thenReturn(departureFlightEntities);
        when(searchFlightRepository.findFlights(
                request.getToAirportId(), request.getFromAirportId(), returnStart, returnEnd, request.toPageable())).thenReturn(returnFlightEntities);

        // Then
        CustomPage<Flight> result = searchFlightService.searchFlights(request);

        assertNotNull(result, "Result should not be null");
        assertEquals(departureFlights.size() + returnFlights.size(), result.getContent().size(), "Total flights should match the sum of departure and return flights");

        assertEquals(departureFlights.get(0).getFromAirport().getId(),
                result.getContent().get(0).getFromAirport().getId(), "Departure flight from airport ID should match");
        assertEquals(departureFlights.get(0).getToAirport().getId(),
                result.getContent().get(0).getToAirport().getId(), "Departure flight to airport ID should match");
        assertEquals(returnFlights.get(0).getFromAirport().getId(),
                result.getContent().get(1).getFromAirport().getId(), "Return flight from airport ID should match");
        assertEquals(returnFlights.get(0).getToAirport().getId(),
                result.getContent().get(1).getToAirport().getId(), "Return flight to airport ID should match");

        // Verify
        verify(searchFlightRepository).findFlights(request.getFromAirportId(), request.getToAirportId(),
                departureStart, departureEnd, request.toPageable());
        verify(searchFlightRepository).findFlights(request.getToAirportId(), request.getFromAirportId(),
                returnStart, returnEnd, request.toPageable());

    }

    @Test
    void givenSearchFlightRequest_whenNoFlightFound_thenReturnEmptyList() {

        // Given
        final CustomPaging pagination = CustomPaging.builder()
                .pageNumber(1)
                .pageSize(10)
                .build();

        final SearchFlightRequest request = SearchFlightRequest.builder()
                .fromAirportId(UUID.randomUUID().toString())
                .toAirportId(UUID.randomUUID().toString())
                .departureTime(LocalDateTime.of(2025, 1, 19, 10, 0))
                .pagination(pagination)
                .build();

        final LocalDateTime departureStart = LocalDateTime.of(2025, 1, 19, 0, 0);
        final LocalDateTime departureEnd = departureStart.plusDays(1).minusNanos(1);

        final Page<FlightEntity> emptyPage = new PageImpl<>(Collections.emptyList());
        final List<Flight> emptyFlightList = listFlightEntityToListFlightMapper.toFlightList(emptyPage.getContent());

        // When
        when(searchFlightRepository.findFlights(
                request.getFromAirportId(), request.getToAirportId(), departureStart, departureEnd, request.toPageable())).thenReturn(emptyPage);

        CustomPage<Flight> result = searchFlightService.searchFlights(request);

        // Then
        assertNotNull(result, "Result should not be null");
        assertNotNull(emptyFlightList, "Mapped flight list should not be null");
        assertTrue(emptyFlightList.isEmpty(), "Mapped flight list should be empty");
        assertEquals(emptyFlightList.size(), result.getContent().size(), "Result content size should match empty flight list size");
        assertTrue(result.getContent().isEmpty(), "Result content should be empty");

        // Verify
        verify(searchFlightRepository).findFlights(request.getFromAirportId(), request.getToAirportId(), departureStart, departureEnd, request.toPageable());

    }


}