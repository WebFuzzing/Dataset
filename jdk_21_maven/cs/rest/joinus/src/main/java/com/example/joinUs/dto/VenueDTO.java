package com.example.joinUs.dto;

import com.example.joinUs.model.embedded.CityEmbedded;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VenueDTO {

    private CityEmbedded city;
    private String address1;
    private String address2;

}
