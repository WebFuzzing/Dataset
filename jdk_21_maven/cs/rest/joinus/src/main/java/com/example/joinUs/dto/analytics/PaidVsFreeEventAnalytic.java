package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class PaidVsFreeEventAnalytic {

    @Field("eventsCount")
    public Integer eventsCount;

    @Field("totalAttendance")
    public Integer totalAttendance;

    @Field("avgAttendance")
    public Integer avgAttendance;

    @Field("type")
    public String type;
}
