package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class TopicPerUserAnalytic {

    @Field("topic")
    public String topic;

    @Field("usersCount")
    public Integer usersCount;
}
