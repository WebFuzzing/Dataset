package com.example.demo.flight.controller;

import com.example.demo.base.AbstractRestControllerTest;
import com.example.demo.builder.*;
import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.CustomPaging;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.flight.exception.FlightNotFoundException;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.airport.AirportPagingRequest;
import com.example.demo.flight.model.dto.request.flight.CreateFlightRequest;
import com.example.demo.flight.model.dto.request.flight.UpdateFlightRequest;
import com.example.demo.flight.model.dto.response.flight.FlightResponse;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.CustomPageFlightToCustomPagingFlightResponseMapper;
import com.example.demo.flight.model.mapper.flight.FlightToFlightResponseMapper;
import com.example.demo.flight.service.flight.FlightService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration test class for the {@link FlightController}.
 * This class tests the REST endpoints of the {@link FlightController} to ensure that task-related
 * operations are performed correctly.
 */
class FlightControllerTest extends AbstractRestControllerTest {

    @MockitoBean
    FlightService flightService;

    private final FlightToFlightResponseMapper flightToFlightResponseMapper  =  FlightToFlightResponseMapper.initialize();

    private final CustomPageFlightToCustomPagingFlightResponseMapper customPageFlightToCustomPagingFlightResponseMapper
            = CustomPageFlightToCustomPagingFlightResponseMapper.initialize();

    @Test
    void givenValidCreateFlightRequestByAdmin_whenCreateFlight_thenSuccess() throws Exception {

        // Given
        final LocalDateTime now = LocalDateTime.now();

        final CreateFlightRequest request = new CreateFlightRequestBuilder()
                .withValidFields()
                .withDepartureTime(now.plusDays(1)) // Departure time is 1 day in the future
                .withArrivalTime(now.plusDays(1).plusHours(2)) // Arrival time is 2 hours after departure
                .build();

        final Flight expectedFlight = Flight.builder()
                .id(UUID.randomUUID().toString())
                .fromAirport(Airport.builder().id(request.getFromAirportId()).build())
                .toAirport(Airport.builder().id(request.getToAirportId()).build())
                .departureTime(request.getDepartureTime())
                .arrivalTime(request.getArrivalTime())
                .price(request.getPrice())
                .build();

        // When
        when(flightService.createFlight(any(CreateFlightRequest.class))).thenReturn(expectedFlight);

        // Then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/v1/flights")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken())
                ).andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response").value(expectedFlight.getId()));

        // Verify
        verify(flightService, times(1)).createFlight(any(CreateFlightRequest.class));

    }

    @Test
    void givenValidCreateFlightRequest_whenUnauthorized_thenThrowUnAuthorizedException() throws Exception {

        // Given
        final LocalDateTime now = LocalDateTime.now();

        final CreateFlightRequest request = new CreateFlightRequestBuilder()
                .withValidFields()
                .withDepartureTime(now.plusDays(1)) // Departure time is 1 day in the future
                .withArrivalTime(now.plusDays(1).plusHours(2)) // Arrival time is 2 hours after departure
                .build();

        // Then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/v1/flights")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(flightService, never()).createFlight(any(CreateFlightRequest.class));

    }

    @Test
    void givenValidCreateFlightRequest_whenForbiddenByUser_thenThrowForbidden() throws Exception {

        // Given
        final LocalDateTime now = LocalDateTime.now();

        final CreateFlightRequest request = new CreateFlightRequestBuilder()
                .withValidFields()
                .withDepartureTime(now.plusDays(1)) // Departure time is 1 day in the future
                .withArrivalTime(now.plusDays(1).plusHours(2)) // Arrival time is 2 hours after departure
                .build();

        // Then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/v1/flights")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken())
                                .content(objectMapper.writeValueAsString(request))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        // Verify
        verify(flightService, never()).createFlight(any(CreateFlightRequest.class));

    }

    @Test
    void givenInvalidArrivalTime_whenCreateFlight_thenThrowsValidationException() throws Exception {

        // Given
        final LocalDateTime now = LocalDateTime.now();

        final CreateFlightRequest invalidRequest = new CreateFlightRequestBuilder()
                .withValidFields()
                .withDepartureTime(now.plusDays(1)) // Departure time is 1 day in the future
                .withArrivalTime(now.plusDays(1).minusHours(1)) // Invalid: Arrival time is 1 hour before departure
                .build();

        // Then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .post("/api/v1/flights")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Validation failed"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].message").value("Arrival time must be the same as or later than departure time!"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.subErrors[0].field").value("createFlightRequest"));

        // Verify
        verify(flightService, never()).createFlight(any(CreateFlightRequest.class));

    }

    @Test
    void givenExistFlightId_whenGetFlightByIdWithAdmin_thenReturnCustomResponse() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();

        final Flight mockFlight = new FlightBuilder()
                .withId(mockFlightId)
                .withValidFields()
                .build();

        final FlightResponse expectedResponse = flightToFlightResponseMapper.map(mockFlight);

        // When
        when(flightService.getFlightById(mockFlightId)).thenReturn(mockFlight);

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/flights/{id}", mockFlightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.id").value(expectedResponse.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.id").value(expectedResponse.getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.name").value(expectedResponse.getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.cityName").value(expectedResponse.getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.id").value(expectedResponse.getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.name").value(expectedResponse.getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.cityName").value(expectedResponse.getToAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.price").value(expectedResponse.getPrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"));


        // Verify
        verify(flightService, times(1)).getFlightById(mockFlightId);

    }

    @Test
    void givenExistFlightId_whenGetFlightByIdWithUser_thenReturnCustomResponse() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();

        final Flight mockFlight = new FlightBuilder()
                .withId(mockFlightId)
                .withValidFields()
                .build();

        final FlightResponse expectedResponse = flightToFlightResponseMapper.map(mockFlight);

        // When
        when(flightService.getFlightById(mockFlightId)).thenReturn(mockFlight);

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/flights/{id}", mockFlightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.id").value(expectedResponse.getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.id").value(expectedResponse.getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.name").value(expectedResponse.getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.cityName").value(expectedResponse.getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.id").value(expectedResponse.getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.name").value(expectedResponse.getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.cityName").value(expectedResponse.getToAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.price").value(expectedResponse.getPrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"));


        // Verify
        verify(flightService, times(1)).getFlightById(mockFlightId);

    }

    @Test
    void givenNonExistFlightId_whenGetFlightByIdWithAdmin_thenThrowFlightNotFoundException() throws Exception {

        // Given
        final String nonExistentFlightId = UUID.randomUUID().toString();
        final String expectedMessage = "Flight not found!\n Flight not found with ID: " + nonExistentFlightId;

        // When
        when(flightService.getFlightById(nonExistentFlightId))
                .thenThrow(new FlightNotFoundException("Flight not found with ID: " + nonExistentFlightId));

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/flights/{id}", nonExistentFlightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.header").value("API ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(expectedMessage))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(false));

        // Verify
        verify(flightService, times(1)).getFlightById(nonExistentFlightId);

    }

    @Test
    void givenNonExistFlightId_whenGetFlightByIdWithUser_thenThrowFlightNotFoundException() throws Exception {

        // Given
        final String nonExistentFlightId = UUID.randomUUID().toString();
        final String expectedMessage = "Flight not found!\n Flight not found with ID: " + nonExistentFlightId;

        // When
        when(flightService.getFlightById(nonExistentFlightId))
                .thenThrow(new FlightNotFoundException("Flight not found with ID: " + nonExistentFlightId));

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/flights/{id}", nonExistentFlightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.header").value("API ERROR"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(expectedMessage))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(false));

        // Verify
        verify(flightService, times(1)).getFlightById(nonExistentFlightId);

    }

    @Test
    void givenExistFlightId_whenUserUnauthorized_thenReturnUnauthorized() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();

        final Flight mockFlight = new FlightBuilder()
                .withId(mockFlightId)
                .withValidFields()
                .build();

        // When
        when(flightService.getFlightById(mockFlightId)).thenReturn(mockFlight);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/flights/{id}", mockFlightId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(flightService, never()).getFlightById(mockFlightId);

    }

    @Test
    void givenFlightPagingRequest_whenGetFlightsFromAdmin_thenReturnCustomPageFlight() throws Exception {

        // Given
        final AirportPagingRequest pagingRequest = AirportPagingRequest.builder()
                .pagination(
                        CustomPaging.builder()
                                .pageSize(1)
                                .pageNumber(1)
                                .build()
                ).build();

        final String flightId = UUID.randomUUID().toString();

        final FlightEntity expectedEntity = new FlightEntityBuilder()
                .withId(flightId)
                .withValidFields()
                .build();

        final List<FlightEntity> flightEntities = List.of(expectedEntity);

        final Page<FlightEntity> flightEntityPage = new PageImpl<>(flightEntities, PageRequest.of(1, 1), flightEntities.size());

        final List<Flight> flightDomainModels = flightEntities.stream()
                .map(entity -> new Flight(
                        entity.getId(),
                        new Airport(entity.getFromAirport().getId(), entity.getFromAirport().getName(), entity.getFromAirport().getCityName()),
                        new Airport(entity.getToAirport().getId(), entity.getToAirport().getName(), entity.getToAirport().getCityName()),
                        entity.getDepartureTime(),
                        entity.getArrivalTime(),
                        entity.getPrice()
                ))
                .collect(Collectors.toList());

        final CustomPage<Flight> flightPage = CustomPage.of(flightDomainModels, flightEntityPage);

        final CustomPagingResponse<FlightResponse> expectedResponse =
                customPageFlightToCustomPagingFlightResponseMapper.toPagingResponse(flightPage);

        // When
        when(flightService.getAllFlights(any(AirportPagingRequest.class))).thenReturn(flightPage);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagingRequest))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].id").value(expectedResponse.getContent().get(0).getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.id").value(expectedResponse.getContent().get(0).getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.name").value(expectedResponse.getContent().get(0).getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.cityName").value(expectedResponse.getContent().get(0).getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.id").value(expectedResponse.getContent().get(0).getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.name").value(expectedResponse.getContent().get(0).getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.cityName").value(expectedResponse.getContent().get(0).getToAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].price").value(expectedResponse.getContent().get(0).getPrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.pageNumber").value(expectedResponse.getPageNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.pageSize").value(expectedResponse.getPageSize()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.totalElementCount").value(expectedResponse.getTotalElementCount()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.totalPageCount").value(expectedResponse.getTotalPageCount()));

        // Verify
        verify(flightService, times(1)).getAllFlights(any(AirportPagingRequest.class));

    }

    @Test
    void givenFlightPagingRequest_whenGetFlightsFromUser_thenReturnCustomPageFlight() throws Exception {

        // Given
        final AirportPagingRequest pagingRequest = AirportPagingRequest.builder()
                .pagination(
                        CustomPaging.builder()
                                .pageSize(1)
                                .pageNumber(1)
                                .build()
                ).build();

        final String flightId = UUID.randomUUID().toString();

        final FlightEntity expectedEntity = new FlightEntityBuilder()
                .withId(flightId)
                .withValidFields()
                .build();

        final List<FlightEntity> flightEntities = List.of(expectedEntity);

        final Page<FlightEntity> flightEntityPage = new PageImpl<>(flightEntities, PageRequest.of(1, 1), flightEntities.size());

        final List<Flight> flightDomainModels = flightEntities.stream()
                .map(entity -> new Flight(
                        entity.getId(),
                        new Airport(entity.getFromAirport().getId(), entity.getFromAirport().getName(), entity.getFromAirport().getCityName()),
                        new Airport(entity.getToAirport().getId(), entity.getToAirport().getName(), entity.getToAirport().getCityName()),
                        entity.getDepartureTime(),
                        entity.getArrivalTime(),
                        entity.getPrice()
                ))
                .collect(Collectors.toList());

        final CustomPage<Flight> flightPage = CustomPage.of(flightDomainModels, flightEntityPage);

        final CustomPagingResponse<FlightResponse> expectedResponse =
                customPageFlightToCustomPagingFlightResponseMapper.toPagingResponse(flightPage);

        // When
        when(flightService.getAllFlights(any(AirportPagingRequest.class))).thenReturn(flightPage);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagingRequest))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].id").value(expectedResponse.getContent().get(0).getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.id").value(expectedResponse.getContent().get(0).getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.name").value(expectedResponse.getContent().get(0).getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.cityName").value(expectedResponse.getContent().get(0).getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.id").value(expectedResponse.getContent().get(0).getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.name").value(expectedResponse.getContent().get(0).getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.cityName").value(expectedResponse.getContent().get(0).getToAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].price").value(expectedResponse.getContent().get(0).getPrice()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.pageNumber").value(expectedResponse.getPageNumber()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.pageSize").value(expectedResponse.getPageSize()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.totalElementCount").value(expectedResponse.getTotalElementCount()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.totalPageCount").value(expectedResponse.getTotalPageCount()));

        // Verify
        verify(flightService, times(1)).getAllFlights(any(AirportPagingRequest.class));

    }

    @Test
    void givenFlightPagingRequest_whenUnauthorized_thenThrowUnauthorized() throws Exception {

        // Given
        final AirportPagingRequest pagingRequest = AirportPagingRequest.builder()
                .pagination(
                        CustomPaging.builder()
                                .pageSize(1)
                                .pageNumber(1)
                                .build()
                ).build();

        // Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pagingRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(flightService, never()).getAllFlights(any(AirportPagingRequest.class));

    }

    @Test
    void givenValidUpdateFlightRequest_whenTokenMissing_thenReturnUnauthorized() throws Exception {

        // Given
        final String mockId = UUID.randomUUID().toString();

        final UpdateFlightRequest request = new UpdateFlightRequestBuilder()
                .withValidFields()
                .build();

        // Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/flights/{id}", mockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(flightService, never()).updateFlightById(anyString(), any(UpdateFlightRequest.class));

    }

    @Test
    void givenValidUpdateFlightRequest_whenAdminAuthorized_thenUpdateFlightAndReturnSuccess() throws Exception {
        // Given
        final String mockId = UUID.randomUUID().toString();

        final UpdateFlightRequest request = new UpdateFlightRequestBuilder()
                .withValidFields()
                .withDepartureTime(LocalDateTime.now().plusDays(1))
                .withArrivalTime(LocalDateTime.now().plusDays(2))
                .build();

        final Flight mockFlight = new FlightBuilder()
                .withId(mockId)
                .withFromAirport(new AirportBuilder().withValidFields())
                .withToAirport(new AirportBuilder().withValidFields())
                .withDepartureTime(request.getDepartureTime())
                .withArrivalTime(request.getArrivalTime())
                .withPrice(request.getPrice())
                .build();

        final FlightResponse response = flightToFlightResponseMapper.map(mockFlight);

        when(flightService.updateFlightById(eq(mockId), any(UpdateFlightRequest.class))).thenReturn(mockFlight);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/flights/{id}", mockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.id").value(response.getId())) // Ensure the response ID matches the mocked one
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.id").value(response.getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.name").value(response.getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.fromAirport.cityName").value(response.getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.id").value(response.getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.name").value(response.getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.toAirport.cityName").value(response.getToAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.price").value(response.getPrice()));

        // Verify
        verify(flightService).updateFlightById(eq(mockId), any(UpdateFlightRequest.class));

    }

    @Test
    void givenInvalidFlightId_whenUpdateFlight_thenThrowNotFoundException() throws Exception {

        // Given
        final String nonExistentFlightId = UUID.randomUUID().toString();
        final UpdateFlightRequest request = new UpdateFlightRequestBuilder()
                .withValidFields()
                .build();

        final String expectedMessage = "Flight not found!\n Flight not found with ID: " + nonExistentFlightId;

        // When
        when(flightService.updateFlightById(anyString(), any(UpdateFlightRequest.class)))
                .thenThrow(new FlightNotFoundException("Flight not found with ID: " + nonExistentFlightId));

        // Then
        mockMvc.perform(
                        MockMvcRequestBuilders
                                .put("/api/v1/flights/{id}", nonExistentFlightId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(expectedMessage))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(false));

        // Verify
        verify(flightService, times(1)).updateFlightById(anyString(), any(UpdateFlightRequest.class));

    }

    @Test
    void givenValidUpdateFlightRequest_whenUserUnAuthorized_thenReturnForbidden() throws Exception {

        // Given
        final String mockId = UUID.randomUUID().toString();
        final UpdateFlightRequest request = new UpdateFlightRequestBuilder()
                .withValidFields()
                .build();

        // Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/flights/{id}", mockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken())
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        // Verify
        verify(flightService, never()).updateFlightById(anyString(), any(UpdateFlightRequest.class));

    }

    @Test
    void givenValidUpdateRequest_whenUserNotAuthenticated_thenThrowUnauthorized() throws Exception {

        // Given
        final String mockId = UUID.randomUUID().toString();
        final UpdateFlightRequest request = new UpdateFlightRequestBuilder()
                .withValidFields()
                .build();

        // Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/flights/{id}", mockId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(flightService, never()).updateFlightById(anyString(), any(UpdateFlightRequest.class));

    }

    @Test
    void givenValidFlightId_whenDeleteFlightByIdFromAdmin_thenSuccess() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();

        // When
        doNothing().when(flightService).deleteFlightById(mockFlightId);

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/v1/flights/{id}", mockFlightId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response").value(
                        "Flight with id " + mockFlightId + "is deleted")
                );

        // Verify
        verify(flightService, times(1)).deleteFlightById(mockFlightId);

    }

    @Test
    void givenValidAirportId_whenDeleteAirportByIdFromUser_thenForbidden() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/v1/flights/{id}", mockFlightId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isForbidden());

        // Verify
        verify(flightService, never()).deleteFlightById(mockFlightId);

    }

    @Test
    void givenValidAirportId_whenNotAuthenticated_thenUnauthorized() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/v1/flights/{id}", mockFlightId))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(flightService, never()).deleteFlightById(mockFlightId);

    }

    @Test
    void givenInvalidFlightId_whenAdminDeletesFlight_thenFlightNotFoundException() throws Exception {

        // Given
        final String mockFlightId = UUID.randomUUID().toString();
        final String expectedMessage = "Flight not found!\n";

        // When
        doThrow(new FlightNotFoundException())
                .when(flightService).deleteFlightById(mockFlightId);

        // Then
        mockMvc.perform(MockMvcRequestBuilders
                        .delete("/api/v1/flights/{id}", mockFlightId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("NOT_FOUND"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(false))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value(expectedMessage));

        // Verify
        verify(flightService, times(1)).deleteFlightById(mockFlightId);

    }

}