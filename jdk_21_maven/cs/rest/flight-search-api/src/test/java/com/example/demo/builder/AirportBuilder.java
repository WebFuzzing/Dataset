package com.example.demo.builder;

import com.example.demo.flight.model.Airport;

import java.util.UUID;

/**
 * A builder class for creating instances of {@link Airport} with specific properties set.
 * This builder is intended for creating instances of {@link Airport} with specific configurations.
 */
public class AirportBuilder extends BaseBuilder<Airport>{

    public AirportBuilder() { super(Airport.class);}

    public Airport withValidFields(){
        return this
                .withId(UUID.randomUUID().toString())
                .withName("Airport Name")
                .withCityName("Airport City Name")
                .build();
    }

    public AirportBuilder withId(String id){
        data.setId(id);
        return this;
    }

    public AirportBuilder withName(String name){
        data.setName(name);
        return this;
    }

    public AirportBuilder withCityName(String cityName){
        data.setCityName(cityName);
        return this;
    }

}

