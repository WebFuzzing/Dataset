package com.example.joinUs.mapping.embedded;

import com.example.joinUs.mapping.CentralMappingConfig;
import com.example.joinUs.model.embedded.EventEmbedded;
import com.example.joinUs.model.mongodb.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(config = CentralMappingConfig.class, uses = { GroupEmbeddedMapper.class })
public interface EventEmbeddedMapper {

    @Mapping(target = "eventId", source = "id")
    @Mapping(source = "eventName", target = "eventName")
    @Mapping(source = "eventTime", target = "eventTime")
    EventEmbedded toDTO(Event event);

    @Mapping(target = "eventId", source = "id")
    @Mapping(source = "eventName", target = "eventName")
    @Mapping(source = "eventTime", target = "eventTime")
    List<EventEmbedded> toDTOs(List<Event> events);

}
