package com.example.demo.flight.model;

import com.example.demo.common.model.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Represents an airport in the system with basic properties such as an identifier and a name.
 * This class extends {@link BaseDomainModel} and provides functionality to model a task
 * in the application.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Flight extends BaseDomainModel {

    private String id;
    private Airport fromAirport;
    private Airport toAirport;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Double price;

}
