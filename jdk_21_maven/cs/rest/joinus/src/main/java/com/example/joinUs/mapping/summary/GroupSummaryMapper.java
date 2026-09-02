package com.example.joinUs.mapping.summary;

import com.example.joinUs.dto.CategoryDTO;
import com.example.joinUs.dto.summary.GroupSummaryDTO;
import com.example.joinUs.mapping.CentralMappingConfig;
import com.example.joinUs.mapping.UserMapper;
import com.example.joinUs.mapping.embedded.EventEmbeddedMapper;
import com.example.joinUs.mapping.embedded.GroupEmbeddedMapper;
import com.example.joinUs.mapping.embedded.UserEmbeddedMapper;
import com.example.joinUs.model.mongodb.Group;
import com.example.joinUs.model.neo4j.GroupNeo4J;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(config = CentralMappingConfig.class, uses = {
        UserEmbeddedMapper.class, EventEmbeddedMapper.class, GroupEmbeddedMapper.class,
        UserMapper.class })

public interface GroupSummaryMapper {

    @Mapping(source = "city.name", target = "cityName")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "organizers", target = "organizers")
    GroupSummaryDTO toDTO(Group group);

    //groupId
    @Mapping(source = "city.name", target = "cityName")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "organizers", target = "organizers")
    List<GroupSummaryDTO> toDTOs(List<Group> group);

    @Mapping(source = "groupId", target = "id")
    @Mapping(source = "groupName", target = "groupName")
    @Mapping(source = "cityName", target = "cityName")
    @Mapping(source = "categoryName", target = "category.name")
    @Mapping(target = "upcomingEvents", ignore = true)
    @Mapping(target = "memberCount", ignore = true)
    @Mapping(target = "eventCount", ignore = true)
    @Mapping(target = "organizers", ignore = true)
    GroupSummaryDTO toDTOFromNeo4j(GroupNeo4J group);

    List<GroupSummaryDTO> toDTOsFromNeo4j(List<GroupNeo4J> groupNeo4JS);

    default List<CategoryDTO> mapCategory(String categoryName) {
        if (categoryName == null) return null;

        return List.of(
                CategoryDTO.builder()
                        .name(categoryName)
                        .build()
        );
    }

}
