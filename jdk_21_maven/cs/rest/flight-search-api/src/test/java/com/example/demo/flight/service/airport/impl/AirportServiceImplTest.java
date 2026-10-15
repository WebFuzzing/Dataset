package com.example.demo.flight.service.airport.impl;

import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.builder.AirportBuilder;
import com.example.demo.builder.AirportEntityBuilder;
import com.example.demo.builder.CreateAirportRequestBuilder;
import com.example.demo.builder.UpdateAirportRequestBuilder;
import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.CustomPaging;
import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;
import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;
import com.example.demo.flight.model.entity.AirportEntity;
import com.example.demo.flight.service.airport.AirportCreateService;
import com.example.demo.flight.service.airport.AirportDeleteService;
import com.example.demo.flight.service.airport.AirportReadService;
import com.example.demo.flight.service.airport.AirportUpdateService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link AirportServiceImpl}.
 * This class verifies the correctness of the business logic in {@link AirportServiceImpl}.
 */
class AirportServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private AirportServiceImpl airportService;

    @Mock
    private AirportCreateService airportCreateService;

    @Mock
    private AirportReadService airportReadService;

    @Mock
    private AirportUpdateService airportUpdateService;

    @Mock
    private AirportDeleteService airportDeleteService;

    @Test
    void givenValidCreateAirportRequest_whenCreateAirport_thenReturnCreatedAirport() {

        // Given
        final CreateAirportRequest request = new CreateAirportRequestBuilder()
                .withValidFields().build();

        Airport expectedAirport = Airport.builder()
                .id(UUID.randomUUID().toString())
                .name(request.getName())
                .cityName(request.getCityName())
                .build();

        // When
        when(airportCreateService.createAirport(any(CreateAirportRequest.class))).thenReturn(expectedAirport);

        // Then
        Airport result = airportService.createAirport(request);

        assertNotNull(result);
        assertEquals(expectedAirport.getId(), result.getId());
        assertEquals(expectedAirport.getName(), result.getName());
        assertEquals(expectedAirport.getCityName(), result.getCityName());

        // Verify
        verify(airportCreateService, times(1)).createAirport(any(CreateAirportRequest.class));

    }

    @Test
    void givenValidAirportId_whenGetAirportById_thenReturnAirport() {

        // Given
        String airportId = UUID.randomUUID().toString();
        Airport expectedAirport = Airport.builder()
                .id(airportId)
                .name("Test Airport")
                .cityName("Test City")
                .build();

        // When
        when(airportReadService.getAirportById(airportId)).thenReturn(expectedAirport);

        // Then
        Airport result = airportService.getAirportById(airportId);

        assertNotNull(result);
        assertEquals(expectedAirport.getId(), result.getId());
        assertEquals(expectedAirport.getName(), result.getName());
        assertEquals(expectedAirport.getCityName(), result.getCityName());

        // Verify
        verify(airportReadService, times(1)).getAirportById(airportId);

    }

    @Test
    void givenValidPagingRequest_whenGetAllAirports_thenReturnCustomPageOfAirports() {

        // Given
        CustomPaging paging = CustomPaging.builder()
                .pageSize(1)
                .pageNumber(1)
                .build();

        CustomPagingRequest pagingRequest = mock(CustomPagingRequest.class);

        List<Airport> airportList = List.of(new Airport());
        CustomPage<Airport> expectedPage = CustomPage.<Airport>builder()
                .content(airportList)
                .pageNumber(1)
                .pageSize(1)
                .totalElementCount(1L)
                .totalPageCount(1)
                .build();


        // When
        when(pagingRequest.getPagination()).thenReturn(paging);
        when(pagingRequest.toPageable()).thenReturn(PageRequest.of(paging.getPageNumber(), paging.getPageSize()));
        when(airportReadService.getAllAirports(pagingRequest)).thenReturn(expectedPage);

        // Then
        CustomPage<Airport> result = airportService.getAllAirports(pagingRequest);

        assertNotNull(result);
        assertEquals(expectedPage, result);
        assertEquals(1, result.getContent().size());

        // Verify
        verify(airportReadService, times(1)).getAllAirports(pagingRequest);

    }

    @Test
    void givenUpdateAirportRequest_whenUpdateAirportById_thenSuccess() {

        // Given
        final String mockId = UUID.randomUUID().toString();

        final UpdateAirportRequest mockUpdateAirportRequest = new UpdateAirportRequestBuilder()
                .withValidFields()
                .build();

        final AirportEntity updatedAirportEntity = new AirportEntityBuilder()
                .withId(mockId)
                .withName(mockUpdateAirportRequest.getName())
                .withCityName(mockUpdateAirportRequest.getCityName())
                .build();

        final Airport expected = new AirportBuilder()
                .withId(updatedAirportEntity.getId())
                .withName(updatedAirportEntity.getName())
                .withCityName(updatedAirportEntity.getCityName())
                .build();

        when(airportUpdateService.updateAirportById(mockId, mockUpdateAirportRequest)).thenReturn(expected);

        // When
        Airport result = airportService.updateAirportById(mockId, mockUpdateAirportRequest);

        // Then
        assertNotNull(result);
        assertEquals(expected.getName(), result.getName());
        assertEquals(expected.getCityName(), result.getCityName());

        // Verify
        verify(airportUpdateService,times(1)).updateAirportById(mockId, mockUpdateAirportRequest);

    }

    @Test
    void givenValidAirportId_whenDeleteAirportById_thenDeleteAirportSuccessfully() {

        // Given
        final String airportId = UUID.randomUUID().toString();

        // When
        doNothing().when(airportDeleteService).deleteAirportById(airportId);

        // Then
        airportService.deleteAirportById(airportId);

        // Verify
        verify(airportDeleteService, times(1)).deleteAirportById(airportId);

    }


}