package com.example.joinUs.model.mongodb;

import com.example.joinUs.model.embedded.CityEmbedded;
import com.example.joinUs.model.embedded.EventEmbedded;
import com.example.joinUs.model.embedded.TopicEmbedded;
import com.example.joinUs.model.embedded.UserEmbedded;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;


@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "groups")
public class Group {

    @Id
    @Indexed(unique = true)
    private String id;

    private String description;
    private String groupName;
    private Date created;

    private CityEmbedded city;
    private Category category;
    private GroupPhoto groupPhoto;

    private Integer memberCount;
    private Integer eventCount;

    private List<UserEmbedded> organizers;
    private List<EventEmbedded> upcomingEvents;
    private List<TopicEmbedded> topics;

    public void removeOrganizerMember(String memberId) {
        organizers.removeIf(e -> e.getMemberId().equalsIgnoreCase(memberId));
    }

    public void removeUpcomingEvent(String eventId) {
        upcomingEvents.removeIf(e -> e.getEventId().equalsIgnoreCase(eventId));
    }

    public void addUpcomingEvent(EventEmbedded eventEmbedded) {
        upcomingEvents.add(eventEmbedded);
    }

    public void addOrganizer(UserEmbedded userEmbedded) {
        organizers.add(userEmbedded);
    }

}
