package com.example.demo.flight.controller;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.common.model.dto.response.CustomResponse;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.SearchFlightRequest;
import com.example.demo.flight.model.dto.response.flight.FlightResponse;
import com.example.demo.flight.model.mapper.flight.CustomPageFlightToCustomPagingFlightResponseMapper;
import com.example.demo.flight.service.flight.SearchFlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RestController for searching flights based on given criteria.
 * Provides an endpoint for searching flights with filters such as departure, arrival airports, and date.
 */
@RestController
@RequestMapping("api/v1/flights/search")
@RequiredArgsConstructor
@Validated
@Tag(name = "Search Flight", description = "Endpoints for searching flights based on given criteria.")
public class SearchFlightController {

    private final SearchFlightService searchFlightService;

    private final CustomPageFlightToCustomPagingFlightResponseMapper customPageFlightToCustomPagingFlightResponseMapper =
            CustomPageFlightToCustomPagingFlightResponseMapper.initialize();

    /**
     * Searches for flights based on given criteria.
     *
     * @param request the search criteria and pagination details.
     * @return a paginated response containing a list of matching flights.
     */
    @Operation(
            summary = "Search for flights",
            description = "Searches for flights based on departure, arrival airports, departure date, and optionally return date. Accessible by ADMIN and USER roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flights successfully retrieved"),
                    @ApiResponse(responseCode = "400", description = "Invalid search criteria provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized, authentication is required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public CustomResponse<CustomPagingResponse<FlightResponse>> searchFlights(
            @RequestBody @Valid SearchFlightRequest request) {

        CustomPage<Flight> flightPage = searchFlightService.searchFlights(request);

        CustomPagingResponse<FlightResponse> response =
                customPageFlightToCustomPagingFlightResponseMapper.toPagingResponse(flightPage);

        return CustomResponse.successOf(response);

    }

}
