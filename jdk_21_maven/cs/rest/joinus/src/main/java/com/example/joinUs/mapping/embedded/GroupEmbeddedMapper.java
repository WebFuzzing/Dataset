package com.example.joinUs.mapping.embedded;

import com.example.joinUs.mapping.CentralMappingConfig;
import com.example.joinUs.model.embedded.GroupEmbedded;
import com.example.joinUs.model.mongodb.Group;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(config = CentralMappingConfig.class)
public interface GroupEmbeddedMapper {

    //    // Explicitly ignored fields

    @Mapping(source = "id", target = "groupId")
    @Mapping(target = "groupName", source = "groupName")
    GroupEmbedded toDTO(Group group);

    //    @Mapping(target = "link", ignore = true)
    //    @Mapping(target = "timezone", ignore = true)
    //    @Mapping(target = "created", ignore = true)
    //    @Mapping(target = "city", ignore = true)
    //    @Mapping(target = "categories", ignore = true)
    //    @Mapping(target = "memberCount", ignore = true)
    //    @Mapping(target = "eventCount", ignore = true)
    //    @Mapping(target = "description", ignore = true)
    //    @Mapping(target = "groupPhoto", ignore = true)
    //    @Mapping(target = "organizers", ignore = true)
    //    @Mapping(target = "upcomingEvents", ignore = true)
    //    @Mapping(target = "id", source = "groupId")
    //    @Mapping(target = "groupName", source = "groupName")
    //    Group toEntity(GroupEmbedded groupEmbedded);

}
