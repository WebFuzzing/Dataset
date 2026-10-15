package com.example.demo.flight.model.dto.request.airport;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a request to create a new airport.
 * This class contains fields for the airport's name and the city where it is located.
 * Both fields are mandatory and cannot be left blank.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CreateAirportRequest {

    @NotBlank(message = "Name field cannot be empty")
    private String name;

    @NotBlank(message = "City Name field cannot be empty")
    private String cityName;

}
