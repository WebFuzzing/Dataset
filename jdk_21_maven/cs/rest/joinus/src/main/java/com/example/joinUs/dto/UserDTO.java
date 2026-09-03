package com.example.joinUs.dto;

import com.example.joinUs.model.embedded.CityEmbedded;
import com.example.joinUs.model.embedded.EventEmbedded;
import com.example.joinUs.model.embedded.TopicEmbedded;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {

    private String id;
    private String memberName;
    private String password; // TODO discuss

    private CityEmbedded city;

    private String bio;

    private List<TopicEmbedded> topics;

    private Integer eventCount;
    private Integer groupCount;

    private List<EventEmbedded> upcomingEvents;

}
