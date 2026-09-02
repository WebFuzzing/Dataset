package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;


@Data
@Document
public class TrendingTopicPerCityAnalytic {

    @Field("city")
    public String city;

    @Field("topTopics")
    public List<TopicPerUserAnalytic> topTopics;
}
