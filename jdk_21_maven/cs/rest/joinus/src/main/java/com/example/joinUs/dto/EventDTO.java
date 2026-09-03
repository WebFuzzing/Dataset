package com.example.joinUs.dto;

import com.example.joinUs.model.embedded.GroupEmbedded;
import com.example.joinUs.model.mongodb.Category;
import com.example.joinUs.model.mongodb.Fee;
import com.example.joinUs.model.mongodb.Venue;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventDTO {

    private String id;
    private String eventName;
    private String description;

    private Date created;
    private Date eventTime;
    private Date updated;

    private Integer duration;

    private Fee fee;
    private Venue venue;
    private Category category;

    private Integer memberCount;

    private GroupEmbedded creatorGroup;

}
