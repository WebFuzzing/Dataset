package com.example.demo.flight.model.mapper.airport;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.response.airport.AirportResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for verifying the behavior of the {@link CustomPageAirportToCustomPagingAirportResponseMapper}.
 * It tests the mapping methods, ensuring that they return {@code null} when the input is {@code null}.
 */
class CustomPageAirportToCustomPagingAirportResponseMapperTest {

    private final CustomPageAirportToCustomPagingAirportResponseMapper mapper =
            CustomPageAirportToCustomPagingAirportResponseMapper.initialize();

    @Test
    void testToPagingResponse_WhenAirportPageIsNull() {

        // Test case where airportPage is null
        CustomPage<Airport> airportPage = null;

        CustomPagingResponse<AirportResponse> result = mapper.toPagingResponse(airportPage);

        // Assert that the result is null when airportPage is null
        assertNull(result);

    }

    @Test
    void testToAirportResponseList_WhenAirportsIsNull() {

        // Test case where airports is null
        List<Airport> airports = null;

        List<AirportResponse> result = mapper.toAirportResponseList(airports);

        // Assert that the result is null when airports is null
        assertNull(result);

    }

}