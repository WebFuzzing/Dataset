package com.example.demo.flight.controller;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.common.model.dto.response.CustomResponse;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.airport.AirportPagingRequest;
import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import com.example.demo.flight.model.dto.response.flight.FlightResponse;
import com.example.demo.flight.model.mapper.flight.CustomPageFlightToCustomPagingFlightResponseMapper;
import com.example.demo.flight.model.mapper.flight.FlightToFlightResponseMapper;
import com.example.demo.flight.service.flight.FlightService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * RestController for managing flights.
 * Provides endpoints for creating, retrieving, updating, and deleting flight information.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/flights")
@Validated
@Tag(name = "Flight Management",
        description = "Endpoints for managing flights including creation, retrieval, updating, and deletion.")
public class FlightController {

    private final FlightService flightService;

    private final FlightToFlightResponseMapper flightToFlightResponseMapper  =  FlightToFlightResponseMapper.initialize();

    private final CustomPageFlightToCustomPagingFlightResponseMapper customPageFlightToCustomPagingFlightResponseMapper
            = CustomPageFlightToCustomPagingFlightResponseMapper.initialize();


    /**
     * Creates a new flight and saves it to the database.
     *
     * @param createFlightRequest the request body containing details of the flight to be created.
     * @return a {@link CustomResponse} containing the ID of the newly created flight.
     */
    @Operation(
            summary = "Create a new flight",
            description = "Creates a new flight and saves it to the database. Accessible by ADMIN only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flight successfully created"),
                    @ApiResponse(responseCode = "400", description = "Invalid flight details provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized, authentication is required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden")
            }
    )
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public CustomResponse<String> createFlight(@RequestBody @Valid final CreateFlightRequest createFlightRequest){
        final Flight savedFlight = flightService.createFlight(createFlightRequest);

        return CustomResponse.successOf(savedFlight.getId());
    }

    /**
     * Retrieves a flight by its ID.
     *
     * @param id the ID of the flight to be retrieved.
     * @return a response containing the flight details.
     */
    @Operation(
            summary = "Get flight by ID",
            description = "Retrieves a flight by its ID. Accessible by both ADMIN and USER roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flight successfully retrieved"),
                    @ApiResponse(responseCode = "400", description = "Invalid flight details provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized, authentication is required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Airport not found")
            }
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public CustomResponse<FlightResponse> getFlightById(@PathVariable @Valid @UUID final String id){

        final Flight flight = flightService.getFlightById(id);

        final FlightResponse response = flightToFlightResponseMapper.map(flight);

        return CustomResponse.successOf(response);

    }

    /**
     * Retrieves a paginated list of flights.
     *
     * @param request the request body containing pagination and sorting information.
     * @return a paginated response containing a list of flights.
     */
    @Operation(
            summary = "Get all flights",
            description = "Retrieves a paginated list of flights. Accessible by both ADMIN and USER roles.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flights successfully retrieved"),
                    @ApiResponse(responseCode = "400", description = "Invalid flights details provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized, authentication is required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Flight not found")
            }
    )
    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN','USER')")
    public CustomResponse<CustomPagingResponse<FlightResponse>> getAllFlights(@RequestBody @Valid final AirportPagingRequest request){
        final CustomPage<Flight> flightCustomPage= flightService.getAllFlights(request);

        final CustomPagingResponse<FlightResponse> response = customPageFlightToCustomPagingFlightResponseMapper
                .toPagingResponse(flightCustomPage);

        return CustomResponse.successOf(response);
    }

    /**
     * Updates an existing flight by its ID.
     *
     * @param id the ID of the flight to be updated.
     * @param updateFlightRequest the request body containing the updated flight details.
     * @return a response containing the updated flight details.
     */
    @Operation(
            summary = "Update an flight",
            description = "Updates an existing flight by its ID. Accessible by ADMIN only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flight successfully updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid update details provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized, authentication is required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Flight not found")
            }
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public CustomResponse<FlightResponse> updateFlightById(
            @PathVariable @Valid @UUID final String id,
            @RequestBody @Valid final UpdateFlightRequest updateFlightRequest) {

        final Flight flight = flightService.updateFlightById(id, updateFlightRequest);

        final FlightResponse response = flightToFlightResponseMapper.map(flight);

        return CustomResponse.successOf(response);
    }

    /**
     * Deletes a flight by its ID.
     *
     * @param id the ID of the flight to be deleted.
     * @return a response containing a success message.
     */
    @Operation(
            summary = "Delete an flight",
            description = "Deletes a flight by its ID. Accessible by ADMIN only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Flight successfully deleted"),
                    @ApiResponse(responseCode = "400", description = "Invalid update details provided"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized, authentication is required"),
                    @ApiResponse(responseCode = "403", description = "Access forbidden"),
                    @ApiResponse(responseCode = "404", description = "Flight not found")
            }
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN')")
    public CustomResponse<String> deleteAirportById(@PathVariable @Valid @UUID final String id){

        flightService.deleteFlightById(id);

        return CustomResponse.successOf("Flight with id "+ id + "is deleted");

    }

}
