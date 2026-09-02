package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class GroupsPerTopicAnalytic {

    @Field("topic")
    public String topic;

    @Field("groupsCount")
    public Integer groupsCount;

}
