package com.example.joinUs.model.mongodb;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Fee {

    private String accepts;
    private Integer amount;
    private String description;
    private Boolean isRequired;

    public static Fee getDefault() {
        return new Fee("others", 0, "per person", false);
    }
}
