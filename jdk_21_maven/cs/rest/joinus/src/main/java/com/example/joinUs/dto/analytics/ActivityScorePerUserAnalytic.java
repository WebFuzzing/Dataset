package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class ActivityScorePerUserAnalytic {

    @Field("userId")
    public String userId;

    @Field("userName")
    public String userName;

    @Field("activityScore")
    public Long activityScore;
}
