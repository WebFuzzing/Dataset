package com.example.joinUs.dto;

import com.example.joinUs.model.embedded.CityEmbedded;
import com.example.joinUs.model.embedded.EventEmbedded;
import com.example.joinUs.model.embedded.TopicEmbedded;
import com.example.joinUs.model.embedded.UserEmbedded;
import com.example.joinUs.model.mongodb.Category;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroupDTO {

    private String id;
    private String description;
    private String groupName;
    private Date created;

    private CityEmbedded city;
    private Category category;

    private Integer memberCount;
    private Integer eventCount;

    private List<UserEmbedded> organizers;

    private List<EventEmbedded> upcomingEvents;
    private List<TopicEmbedded> topics;

    private GroupPhotoDTO groupPhoto;

}

