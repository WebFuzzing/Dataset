package com.example.joinUs.model.embedded;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CityEmbedded {

    private String cityId;
    private String name;
    private String country;
    private String state;
}
