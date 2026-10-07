package com.example.demo.flight.service.flight.impl;

import com.example.demo.common.model.CustomPage;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.SearchFlightRequest;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.ListFlightEntityToListFlightMapper;
import com.example.demo.flight.repository.SearchFlightRepository;
import com.example.demo.flight.service.flight.SearchFlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service implementation for searching flights.
 */
@Service
@RequiredArgsConstructor
public class SearchFlightServiceImpl implements SearchFlightService {

    private final SearchFlightRepository searchFlightRepository;

    private final ListFlightEntityToListFlightMapper listFlightEntityToListFlightMapper =
            ListFlightEntityToListFlightMapper.initialize();

    /**
     * Searches for flights based on the given criteria and returns a paginated result.
     *
     * @param request the search criteria including airport IDs, departure, and optional return times.
     * @return a {@link CustomPage} of {@link Flight} with matching flights and pagination details.
     */
    @Override
    public CustomPage<Flight> searchFlights(SearchFlightRequest request) {

        // Convert the given departure time to LocalDateTime representing the start of the day (midnight).
        // departureStart is used to ensure the search includes flights departing at any time during the given date.
        // Example: If request.getDepartureTime() is "2025-01-20T15:30:00", departureStart will be "2025-01-20T00:00:00".
        LocalDateTime departureStart = request.getDepartureTime().toLocalDate().atStartOfDay();

        // Calculate the end of the departure day (just before midnight of the next day).
        // departureEnd ensures that the search captures flights up until the last moment of the specified departure date.
        // Example: If departureStart is "2025-01-20T00:00:00", departureEnd will be "2025-01-20T23:59:59.999999999".
        LocalDateTime departureEnd = departureStart.plusDays(1).minusNanos(1);

        Pageable pageable = request.toPageable();

        // Fetch one-way flights
        Page<FlightEntity> departureFlightEntities = searchFlightRepository.findFlights(
                request.getFromAirportId(),
                request.getToAirportId(),
                departureStart,
                departureEnd,
                pageable);

        List<Flight> flightList = listFlightEntityToListFlightMapper.toFlightList(departureFlightEntities.getContent());

        // Handle round-trip flights if arrivalTime is provided
        Optional.ofNullable(request.getArrivalTime()).ifPresent(arrivalTime -> {

            // Convert the given arrival time to LocalDateTime representing the start of the day (midnight).
            // returnStart ensures the search includes return flights arriving at any time during the given arrival date.
            // Example: If arrivalTime is "2025-01-25T10:45:00", returnStart will be "2025-01-25T00:00:00".
            LocalDateTime returnStart = arrivalTime.toLocalDate().atStartOfDay();

            // Calculate the end of the return day (just before midnight of the next day).
            // returnEnd ensures that the search captures return flights up until the last moment of the specified arrival date.
            // Example: If returnStart is "2025-01-25T00:00:00", returnEnd will be "2025-01-25T23:59:59.999999999".
            LocalDateTime returnEnd = returnStart.plusDays(1).minusNanos(1);

            // Fetch return flights
            Page<FlightEntity> returnFlightEntities = searchFlightRepository.findFlights(
                    request.getToAirportId(),
                    request.getFromAirportId(),
                    returnStart,
                    returnEnd,
                    pageable);

            List<Flight> returnFlightList =
                    listFlightEntityToListFlightMapper.toFlightList(returnFlightEntities.getContent());

            // Add return flights to the flight list
            flightList.addAll(returnFlightList);

        });

        return CustomPage.of(flightList, departureFlightEntities);

    }

}
