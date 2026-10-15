package com.example.demo.builder;

import com.example.demo.flight.model.dto.request.airport.CreateAirportRequest;

/**
 * A builder class for creating instances of {@link CreateAirportRequest} with specific properties set.
 */
public class CreateAirportRequestBuilder extends BaseBuilder<CreateAirportRequest> {

    public CreateAirportRequestBuilder() { super(CreateAirportRequest.class);}

    public CreateAirportRequestBuilder withValidFields(){
        return this.withName("Airport Name")
                   .withCityName("Airport City Name");
    }

    public CreateAirportRequestBuilder withName(String name){
        data.setName(name);
        return this;
    }

    public CreateAirportRequestBuilder withCityName(String cityName){
        data.setCityName(cityName);
        return this;
    }

}