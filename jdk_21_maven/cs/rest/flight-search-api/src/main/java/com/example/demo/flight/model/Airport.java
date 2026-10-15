package com.example.demo.flight.model;

import com.example.demo.common.model.BaseDomainModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents an airport in the system, with properties such as its identifier, name, and city.
 * Extends {@link BaseDomainModel} to inherit common domain model attributes and behavior.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@SuperBuilder
public class Airport extends BaseDomainModel {

    private String id;

    private String name;

    private String cityName;
}
