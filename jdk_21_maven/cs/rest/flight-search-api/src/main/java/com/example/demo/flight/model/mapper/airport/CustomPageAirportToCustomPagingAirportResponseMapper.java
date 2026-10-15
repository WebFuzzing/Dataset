package com.example.demo.flight.model.mapper.airport;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.flight.model.Airport;
import com.example.demo.flight.model.dto.response.airport.AirportResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface that converts a {@link CustomPage} of {@link Airport} entities to a {@link CustomPagingResponse} of {@link AirportResponse}.
 * It leverages MapStruct for automatic mapping between domain and DTO objects.
 */
@Mapper
public interface CustomPageAirportToCustomPagingAirportResponseMapper {

    AirportToAirportResponseMapper airportToAirportResponseMapper = Mappers.getMapper(AirportToAirportResponseMapper.class);

    /**
     * Converts a {@link CustomPage} of {@link Airport} entities to a {@link CustomPagingResponse} containing {@link AirportResponse} DTOs.
     *
     * @param airportPage the {@link CustomPage} containing a list of {@link Airport} entities
     * @return a {@link CustomPagingResponse} with the mapped {@link AirportResponse} list, or {@code null} if {@code airportPage} is {@code null}
     */
    default CustomPagingResponse<AirportResponse> toPagingResponse(CustomPage<Airport> airportPage) {

        if (airportPage == null) {
            return null;
        }

        return CustomPagingResponse.<AirportResponse>builder()
                .content(toAirportResponseList(airportPage.getContent()))
                .totalElementCount(airportPage.getTotalElementCount())
                .totalPageCount(airportPage.getTotalPageCount())
                .pageNumber(airportPage.getPageNumber())
                .pageSize(airportPage.getPageSize())
                .build();

    }

    /**
     * Converts a list of {@link Airport} entities to a list of {@link AirportResponse} DTOs.
     *
     * @param airports the list of {@link Airport} entities
     * @return a list of {@link AirportResponse} DTOs, or {@code null} if {@code airports} is {@code null}
     */
    default List<AirportResponse> toAirportResponseList(List<Airport> airports) {

        if (airports == null) {
            return null;
        }

        return airports.stream()
                .map(airportToAirportResponseMapper::map)
                .collect(Collectors.toList());

    }

    /**
     * Initializes and returns an instance of the {@link CustomPageAirportToCustomPagingAirportResponseMapper}.
     *
     * @return an instance of the mapper
     */
    static CustomPageAirportToCustomPagingAirportResponseMapper initialize() {
        return Mappers.getMapper(CustomPageAirportToCustomPagingAirportResponseMapper.class);
    }

}
