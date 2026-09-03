package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class GroupsPerCityAnalytic {

    @Field("_id")
    public String _id;

    @Field("groupsCreated")
    public Integer groupsCreated;

    @Field("city")
    public String city;
}
