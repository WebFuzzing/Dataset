package com.jphaugla.domain;

import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class Merchant implements Serializable {

    private String name;
    private String categoryCode;
    private String categoryDescription;
    private String state;
    private String countryCode;
}
