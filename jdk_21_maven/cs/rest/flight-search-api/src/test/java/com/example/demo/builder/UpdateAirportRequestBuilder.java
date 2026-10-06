package com.example.demo.builder;

import com.example.demo.flight.model.dto.request.airport.UpdateAirportRequest;

/**
 * A builder class for creating instances of {@link UpdateAirportRequestBuilder} with specific properties set.
 */
public class UpdateAirportRequestBuilder extends BaseBuilder<UpdateAirportRequest> {

    public UpdateAirportRequestBuilder() { super(UpdateAirportRequest.class);}

    public UpdateAirportRequestBuilder withValidFields(){
        return this.withName("Airport Name")
                .withCityName("Airport City Name");
    }

    public UpdateAirportRequestBuilder withName(String name){
        data.setName(name);
        return this;
    }

    public UpdateAirportRequestBuilder withCityName(String cityName){
        data.setCityName(cityName);
        return this;
    }

}
