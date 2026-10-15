package com.example.demo.flight.model.dto.request.airport;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Request class used to update an airport.
 * Contains fields 'name' and 'cityName' which represent airport's fields to be updated.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class UpdateAirportRequest {

    @NotBlank(message = "name field cannot be empty")
    private String name;

    @NotBlank(message = "cityName field cannot be empty")
    private String cityName;

}
