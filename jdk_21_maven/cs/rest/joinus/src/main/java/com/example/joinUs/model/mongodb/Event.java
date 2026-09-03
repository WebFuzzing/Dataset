package com.example.joinUs.model.mongodb;

import com.example.joinUs.model.embedded.GroupEmbedded;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "events")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Event {

    @Id
    @Indexed(unique = true)
    private String id;
    private String description;
    private String eventName;

    private Date created;
    private Date eventTime;
    private Date updated;

    private Integer duration;
    private Fee fee;
    private Venue venue;
    private Category category;
    private Integer memberCount;
    @Field("creator_group")
    private GroupEmbedded creatorGroup;

}

