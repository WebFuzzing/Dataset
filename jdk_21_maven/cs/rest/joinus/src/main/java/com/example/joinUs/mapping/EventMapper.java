package com.example.joinUs.mapping;

//import com.example.joinUs.dto.EventDTO;
//import com.example.joinUs.model.mongodb.Event;

//package com.example.joinUs.dto;

import com.example.joinUs.dto.EventDTO;
import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.mapping.embedded.GroupEmbeddedMapper;
import com.example.joinUs.mapping.embedded.TopicEmbeddedMapper;
import com.example.joinUs.model.mongodb.Event;
import com.example.joinUs.model.neo4j.EventNeo4J;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Date;


@Mapper(config = CentralMappingConfig.class, uses = {
        CategoryMapper.class
        , GroupEmbeddedMapper.class
        , VenueMapper.class, TopicEmbeddedMapper.class }
        // TODO
)
public interface EventMapper {

    @Mapping(target = "id", source = "id")
    EventDTO toDTO(Event event);

    @Mapping(target = "id", source = "id")
    Event toEntity(EventDTO dto);

    @Mapping(source = "feeAmount", target = "feeAmount")
    @Mapping(source = "cityName", target = "venueCityName")
    @Mapping(source = "eventId", target = "id")
    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "creatorGroup", ignore = true)
    EventSummaryDTO toDTOFromNeo4j(EventNeo4J event);

    @Mapping(target = "feeAmount", source = "fee.amount")
    @Mapping(target = "cityName", source = "venue.city.name")
    @Mapping(target = "groupName", source = "creatorGroup.groupName")
    @Mapping(target = "groupId", source = "creatorGroup.groupId")
    @Mapping(target = "eventId", source = "id")
    @Mapping(target = "venueAddress1", source = "venue.address1")
    EventNeo4J toNeo4jEntity(EventDTO event);

    //    @Mapping(source = "feeAmount", target = "fee.amount")
    //    @Mapping(source = "cityName", target = "venue.city.name")
    //    @Mapping(target = "created", ignore = true)
    //    @Mapping(target = "duration", ignore = true)
    //    @Mapping(target = "category", ignore = true)
    //    @Mapping(target = "memberCount", ignore = true)
    //    @Mapping(source = "groupName", target = "creatorGroup.groupName")
    //    @Mapping(source = "groupId", target = "creatorGroup.groupId")
    //    @Mapping(source = "eventId", target = "id")
    //    @Mapping(source = "eventName", target = "eventName")
    //    @Mapping(source = "eventTime", target = "eventTime")
    //    @Mapping(source = "description", target = "description")
    //    @Mapping(target = "venueAddress1", source = "venue.address1")
    //    List<EventDTO> toDTOsFromNeo4j(List<EventNeo4J> events);

    default Date map(OffsetDateTime value) {
        return value == null ? null : Date.from(value.toInstant());
    }

    default OffsetDateTime map(Date value) {
        return value == null ? null : OffsetDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }

}