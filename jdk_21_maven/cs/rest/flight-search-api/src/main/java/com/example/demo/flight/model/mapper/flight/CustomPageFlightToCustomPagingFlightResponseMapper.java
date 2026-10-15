package com.example.demo.flight.model.mapper.flight;

import com.example.demo.common.model.CustomPage;
import com.example.demo.common.model.dto.response.CustomPagingResponse;
import com.example.demo.flight.model.Flight;
import com.example.demo.flight.model.dto.response.flight.FlightResponse;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper interface that converts a {@link CustomPage} of {@link Flight} entities to a {@link CustomPagingResponse} of {@link FlightResponse}.
 * It leverages MapStruct for automatic mapping between domain and DTO objects.
 */
@Mapper
public interface CustomPageFlightToCustomPagingFlightResponseMapper {

    FlightToFlightResponseMapper flighttoFlightResponseMapper  = Mappers.getMapper(FlightToFlightResponseMapper.class);

    /**
     * Converts a {@link CustomPage} of {@link Flight} entities to a {@link CustomPagingResponse} containing {@link FlightResponse} DTOs.
     *
     * @param flightPage the {@link CustomPage} containing a list of {@link Flight} entities
     * @return a {@link CustomPagingResponse} with the mapped {@link FlightResponse} list, or {@code null} if {@code flightPage} is {@code null}
     */
    default CustomPagingResponse<FlightResponse> toPagingResponse(CustomPage<Flight> flightPage) {

        if (flightPage == null) {
            return null;
        }

        return CustomPagingResponse.<FlightResponse>builder()
                .content(toFlightResponseList(flightPage.getContent()))
                .totalElementCount(flightPage.getTotalElementCount())
                .totalPageCount(flightPage.getTotalPageCount())
                .pageNumber(flightPage.getPageNumber())
                .pageSize(flightPage.getPageSize())
                .build();

    }

    /**
     * Converts a list of {@link Flight} entities to a list of {@link FlightResponse} DTOs.
     *
     * @param flights the list of {@link Flight} entities
     * @return a list of {@link FlightResponse} DTOs, or {@code null} if {@code flights} is {@code null}
     */
    default List<FlightResponse> toFlightResponseList(List<Flight> flights) {

        if (flights == null) {
            return null;
        }

        return flights.stream()
                .map(flighttoFlightResponseMapper::map)
                .collect(Collectors.toList());

    }

    /**
     * Initializes and returns an instance of the {@link CustomPageFlightToCustomPagingFlightResponseMapper}.
     *
     * @return an instance of the mapper
     */
    static CustomPageFlightToCustomPagingFlightResponseMapper initialize() {
        return Mappers.getMapper(CustomPageFlightToCustomPagingFlightResponseMapper.class);
    }


}
