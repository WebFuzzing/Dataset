package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class UsersPerCityAnalytic {

    @Field("city")
    public String city;

    @Field("usersCount")
    public Integer usersCount;
}
