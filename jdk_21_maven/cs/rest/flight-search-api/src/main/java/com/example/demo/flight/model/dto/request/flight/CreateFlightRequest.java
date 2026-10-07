package com.example.demo.flight.model.dto.request.flight;

import com.example.demo.flight.utils.annotations.ValidArrivalTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a request to create a new flight.
 * This class contains fields for the flight's origin, destination, departure time, arrival time, and price.
 * All fields are mandatory and must meet the specified validation constraints.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ValidArrivalTime
public class CreateFlightRequest {

    @NotBlank(message = "From airport ID field cannot be empty")
    private String fromAirportId;

    @NotBlank(message = "To airport ID field cannot be empty")
    private String toAirportId;

    @NotNull(message = "Departure time field cannot be null")
    private LocalDateTime departureTime;

    @NotNull(message = "Arrival time field cannot be null")
    private LocalDateTime arrivalTime;

    @NotNull(message = "Price field cannot be null")
    @Positive(message = "Price must be greater than zero")
    private Double price;

}
