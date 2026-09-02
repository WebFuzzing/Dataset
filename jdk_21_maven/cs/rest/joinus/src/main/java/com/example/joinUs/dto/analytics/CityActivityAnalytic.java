package com.example.joinUs.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CityActivityAnalytic {

    @Field("city")
    private String city;

    @Field("totalMembers")
    private Integer totalMembers;

    @Field("activeMembers")
    private Integer activeMembers;

    @Field("activityRatio")
    private Double activityRatio;
}