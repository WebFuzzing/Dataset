package com.example.joinUs.mapping;

import com.example.joinUs.dto.GroupDTO;
import com.example.joinUs.mapping.embedded.EventEmbeddedMapper;
import com.example.joinUs.mapping.embedded.UserEmbeddedMapper;
import com.example.joinUs.model.mongodb.Group;
import com.example.joinUs.model.neo4j.GroupNeo4J;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;


@Mapper(config = CentralMappingConfig.class,
        uses = { CityMapper.class, CategoryMapper.class, EventEmbeddedMapper.class,
                GroupPhotoMapper.class, UserEmbeddedMapper.class },
        nullValueMappingStrategy = NullValueMappingStrategy.RETURN_NULL // TODO others also
)
public interface GroupMapper {

    //    @Mapping(target = "groupId",source = "id")
    //    @Mapping(source = "id", ignore = true)
    GroupDTO toDTO(Group group);

    //    @Mapping(target = "groupId",source = "id")
    //@Mapping(target = "id", ignore = true)
    List<GroupDTO> toDTOs(List<Group> groups);

    // Ignore Mongo internal id because the DTO doesn't carry it.
    //    @Mapping(target = "id",source = "groupId")
    //    @Mapping(target = "id", ignore = true)
    Group toEntity(GroupDTO dto);

    @Mapping(target = "groupId", source = "id")
    @Mapping(target = "cityName", source = "city.name")
    @Mapping(target = "categoryName", source = "category.name")
    GroupNeo4J toNeo4JEntity(GroupDTO dto);

    @Mapping(target = "id", source = "groupId")
    @Mapping(target = "city.name", source = "cityName")
    @Mapping(target = "category.name", source = "categoryName")
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "eventCount", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    @Mapping(target = "upcomingEvents", ignore = true)
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "groupPhoto", ignore = true)
    GroupDTO toNeo4JDTO(GroupNeo4J groupNeo4J);

}