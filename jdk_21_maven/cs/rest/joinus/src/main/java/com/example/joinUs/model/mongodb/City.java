package com.example.joinUs.model.mongodb;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "cities")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class City {

    @Id
    @Field("_id")
    private String id;
    private String name;

    private String country;
    private String state;

    private String latitude;
    private String longitude;
    private String distance;

}
