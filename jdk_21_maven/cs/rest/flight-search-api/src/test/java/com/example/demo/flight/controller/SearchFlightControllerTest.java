package com.example.demo.flight.controller;

import com.example.demo.base.AbstractRestControllerTest;
import com.example.demo.builder.AirportEntityBuilder;
import com.example.demo.builder.FlightEntityBuilder;
import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.CustomPaging;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.request.flight.SearchFlightRequest;
import com.example.demo.flight.model.dto.response.flight.FlightResponse;
import com.example.demo.flight.model.entity.FlightEntity;
import com.example.demo.flight.model.mapper.flight.CustomPageFlightToCustomPagingFlightResponseMapper;
import com.example.demo.flight.service.flight.SearchFlightService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.testcontainers.shaded.com.google.common.net.HttpHeaders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link SearchFlightController}.
 * This class tests the REST API endpoints of the {@link SearchFlightController}, ensuring that the
 * search flight functionality works as expected. It validates the integration between the controller,
 * the {@link SearchFlightService}, and the mapping logic provided by the
 * {@link CustomPageFlightToCustomPagingFlightResponseMapper}.
 */
class SearchFlightControllerTest extends AbstractRestControllerTest {

    @MockitoBean
    SearchFlightService searchFlightService;

    private final CustomPageFlightToCustomPagingFlightResponseMapper customPageFlightToCustomPagingFlightResponseMapper =
            CustomPageFlightToCustomPagingFlightResponseMapper.initialize();

    @Test
    void givenSearchFlightRequest_whenOneWayFlightsFoundFromAdmin_thenReturnFlights() throws Exception {

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

        final List<FlightEntity> flightEntities = List.of(flightEntity);

        final Page<FlightEntity> flightEntityPage = new PageImpl<>(flightEntities, PageRequest.of(0, 1), flightEntities.size());

        final List<Flight> flightDomainModels = flightEntities.stream()
                .map(entity -> Flight.builder()
                        .id(entity.getId())
                        .fromAirport(Airport.builder()
                                .id(entity.getFromAirport().getId())
                                .name(entity.getFromAirport().getName())
                                .cityName(entity.getFromAirport().getCityName())
                                .build())
                        .toAirport(Airport.builder()
                                .id(entity.getToAirport().getId())
                                .name(entity.getToAirport().getName())
                                .cityName(entity.getToAirport().getCityName())
                                .build())
                        .departureTime(entity.getDepartureTime())
                        .arrivalTime(entity.getArrivalTime())
                        .price(entity.getPrice())
                        .build())
                .collect(Collectors.toList());


        final CustomPage<Flight> flightPage = CustomPage.of(flightDomainModels, flightEntityPage);

        final CustomPagingResponse<FlightResponse> expectedResponse =
                customPageFlightToCustomPagingFlightResponseMapper.toPagingResponse(flightPage);

        // When
        when(searchFlightService.searchFlights(any(SearchFlightRequest.class))).thenReturn(flightPage);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/flights/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockAdminToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.id").value(expectedResponse.getContent().get(0).getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.name").value(expectedResponse.getContent().get(0).getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.cityName").value(expectedResponse.getContent().get(0).getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.id").value(expectedResponse.getContent().get(0).getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.name").value(expectedResponse.getContent().get(0).getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.cityName").value(expectedResponse.getContent().get(0).getToAirport().getCityName()));

        // Verify
        verify(searchFlightService, times(1)).searchFlights(any(SearchFlightRequest.class));

    }

    @Test
    void givenSearchFlightRequest_whenOneWayFlightsFoundFromUser_thenReturnFlights() throws Exception {

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

        final List<FlightEntity> flightEntities = List.of(flightEntity);

        final Page<FlightEntity> flightEntityPage = new PageImpl<>(flightEntities, PageRequest.of(0, 1), flightEntities.size());

        final List<Flight> flightDomainModels = flightEntities.stream()
                .map(entity -> Flight.builder()
                        .id(entity.getId())
                        .fromAirport(Airport.builder()
                                .id(entity.getFromAirport().getId())
                                .name(entity.getFromAirport().getName())
                                .cityName(entity.getFromAirport().getCityName())
                                .build())
                        .toAirport(Airport.builder()
                                .id(entity.getToAirport().getId())
                                .name(entity.getToAirport().getName())
                                .cityName(entity.getToAirport().getCityName())
                                .build())
                        .departureTime(entity.getDepartureTime())
                        .arrivalTime(entity.getArrivalTime())
                        .price(entity.getPrice())
                        .build())
                .collect(Collectors.toList());


        final CustomPage<Flight> flightPage = CustomPage.of(flightDomainModels, flightEntityPage);

        final CustomPagingResponse<FlightResponse> expectedResponse =
                customPageFlightToCustomPagingFlightResponseMapper.toPagingResponse(flightPage);

        // When
        when(searchFlightService.searchFlights(any(SearchFlightRequest.class))).thenReturn(flightPage);

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/flights/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + mockUserToken.getAccessToken()))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.httpStatus").value("OK"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.isSuccess").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.id").value(expectedResponse.getContent().get(0).getFromAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.name").value(expectedResponse.getContent().get(0).getFromAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].fromAirport.cityName").value(expectedResponse.getContent().get(0).getFromAirport().getCityName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.id").value(expectedResponse.getContent().get(0).getToAirport().getId()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.name").value(expectedResponse.getContent().get(0).getToAirport().getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.response.content[0].toAirport.cityName").value(expectedResponse.getContent().get(0).getToAirport().getCityName()));

        // Verify
        verify(searchFlightService, times(1)).searchFlights(any(SearchFlightRequest.class));

    }

    @Test
    void givenSearchFlightRequest_whenUnauthorized_thenThrowUnauthorized() throws Exception {

        // Given
        final CustomPaging pagination = CustomPaging.builder()
                .pageNumber(1)
                .pageSize(10)
                .build();

        final SearchFlightRequest searchFlightRequest = SearchFlightRequest.builder()
                .fromAirportId(UUID.randomUUID().toString())
                .toAirportId(UUID.randomUUID().toString())
                .departureTime(LocalDateTime.of(2025, 1, 19, 10, 0))
                .pagination(pagination)
                .build();

        // Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/flights/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchFlightRequest)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());

        // Verify
        verify(searchFlightService, never()).searchFlights(any(SearchFlightRequest.class));

    }


}