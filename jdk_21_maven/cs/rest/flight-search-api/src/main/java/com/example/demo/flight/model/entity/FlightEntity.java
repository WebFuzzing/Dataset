package com.example.demo.flight.model.entity;

import com.example.demo.common.model.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Represents a flight entity stored in the MongoDB {@code flight-collection}.
 * Contains details about the flight, including departure and destination airports,
 * departure time, and price.
 * Extends {@link BaseEntity} for common timestamp fields.
 */
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "flight-collection")
public class FlightEntity extends BaseEntity {

    @Id
    @Indexed(unique = true)
    private String id;

    @DBRef
    @Field(name = "FROM_AIRPORT")
    private AirportEntity fromAirport;

    @DBRef
    @Field(name = "TO_AIRPORT")
    private AirportEntity toAirport;

    @Field(name = "DEPARTURE_TIME")
    private LocalDateTime departureTime;

    @Field(name = "ARRIVAL_TIME")
    private LocalDateTime arrivalTime;

    @Field(name = "PRICE")
    private Double price;

}
