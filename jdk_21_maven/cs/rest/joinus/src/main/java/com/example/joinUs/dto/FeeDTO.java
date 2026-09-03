package com.example.joinUs.dto;

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
public class FeeDTO {

    private String accepts;
    private Integer amount;
    private String description;
    private Boolean isRequired;

    public static FeeDTO getDefaultDTO() {
        return new FeeDTO("others", 0, "per person", false);
    }
}
