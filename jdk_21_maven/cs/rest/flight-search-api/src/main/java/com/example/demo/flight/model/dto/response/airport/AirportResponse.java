package com.example.demo.flight.model.dto.response.airport;

import lombok.*;

/**
 * Response class representing an airport.
 * This class is used to send information about a airport, including its ID,name and city name.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class AirportResponse {

    private String id;
    private String name;
    private String cityName;
}
