package com.example.joinUs.model.neo4j;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.time.OffsetDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Node("Event")
public class EventNeo4J {

    @Id
    @Property(name = "event_id")
    private String eventId;
    @Property(name = "event_name")
    private String eventName;
    @Property(name = "group_name")
    private String groupName;
    @Property(name = "group_id")
    private String groupId;
    @Property(name = "description")
    private String description;
    @Property(name = "event_time")
    private OffsetDateTime eventTime;
    @Property(name = "fee_amount")
    private String feeAmount;
    @Property(name = "venue_city")
    private String cityName;
    @Property(name = "venue_address_1")
    private String venueAddress1;

}

