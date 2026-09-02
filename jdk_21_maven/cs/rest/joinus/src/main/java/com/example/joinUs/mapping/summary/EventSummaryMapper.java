package com.example.joinUs.mapping.summary;

import com.example.joinUs.dto.summary.EventSummaryDTO;
import com.example.joinUs.mapping.CentralMappingConfig;
import com.example.joinUs.mapping.FeeMapper;
import com.example.joinUs.mapping.VenueMapper;
import com.example.joinUs.mapping.embedded.GroupEmbeddedMapper;
import com.example.joinUs.model.mongodb.Event;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(config = CentralMappingConfig.class, uses = {
        VenueMapper.class, FeeMapper.class,
        GroupEmbeddedMapper.class })
public interface EventSummaryMapper {

    @Mapping(source = "fee.amount", target = "feeAmount")
    @Mapping(source = "venue.city.name", target = "venueCityName")
    @Mapping(source = "venue.address1", target = "venueAddress1")
    @Mapping(source = "id", target = "id")
        //    @Mapping(source = "creatorGroup.id", target = "creatorGroup.groupId")
        //    @Mapping(source = "creatorGroup.groupName", target = "creatorGroup.groupName")
    EventSummaryDTO toDTO(Event event);

    @Mapping(source = "fee.amount", target = "feeAmount")
    @Mapping(source = "venue.city.name", target = "venueCityName")
    @Mapping(source = "venue.address1", target = "venueAddress1")
    @Mapping(source = "id", target = "id")
        //    @Mapping(source = "creatorGroup.id", target = "creatorGroup.groupId")
        //    @Mapping(source = "creatorGroup.groupName", target = "creatorGroup.groupName")
    List<EventSummaryDTO> toDTOs(List<Event> events);

}
