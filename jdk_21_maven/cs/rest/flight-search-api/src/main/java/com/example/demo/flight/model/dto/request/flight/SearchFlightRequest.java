package com.example.demo.flight.model.dto.request.flight;

import com.example.demo.common.model.dto.request.CustomPagingRequest;
import com.example.demo.flight.utils.CustomLocalDateTimeDeserializer;
import com.example.demo.flight.utils.annotations.ValidArrivalTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Request class used for searching flights with specific criteria.
 * Inherits from the base class {@link CustomPagingRequest}, which provides pagination details such as page number and size.
 * This class is used to request a paginated list of flights based on specific search parameters.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@ValidArrivalTime
public class SearchFlightRequest extends CustomPagingRequest {

    @NotNull
    private String fromAirportId;

    @NotNull
    private String toAirportId;

    @NotNull
    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime departureTime;

    @JsonDeserialize(using = CustomLocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime arrivalTime; // Nullable for one-way flights

}
