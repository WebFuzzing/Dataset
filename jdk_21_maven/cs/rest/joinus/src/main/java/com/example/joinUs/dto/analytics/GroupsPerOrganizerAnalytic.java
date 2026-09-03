package com.example.joinUs.dto.analytics;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;


@Data
public class GroupsPerOrganizerAnalytic {

    @Field("userId")
    public String userId;

    @Field("organizerName")
    public String organizerName;

    @Field("groupsOrganized")
    public Integer groupsOrganized;
}
