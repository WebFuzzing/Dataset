package com.example.demo.builder;

import com.example.demo.flight.model.entity.AirportEntity;

import java.util.UUID;

/**
 * A builder class for creating instances of {@link AirportEntity} with specific properties set.
 * This builder is intended for creating instances of {@link AirportEntity} with specific configurations.
 */
public class AirportEntityBuilder extends BaseBuilder<AirportEntity>{

    public AirportEntityBuilder() { super(AirportEntity.class);}

    public AirportEntity withValidFields(){
        return this
                .withId(UUID.randomUUID().toString())
                .withName("Airport Name")
                .withCityName("Airport City Name")
                .build();
    }

    public AirportEntityBuilder withId(String id){
        data.setId(id);
        return this;
    }

    public AirportEntityBuilder withName(String name){
        data.setName(name);
        return this;
    }

    public AirportEntityBuilder withCityName(String cityName){
        data.setCityName(cityName);
        return this;
    }

}
