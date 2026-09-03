package com.example.joinUs.mapping;

import com.example.joinUs.dto.UserDTO;
import com.example.joinUs.mapping.embedded.EventEmbeddedMapper;
import com.example.joinUs.mapping.embedded.TopicEmbeddedMapper;
import com.example.joinUs.model.mongodb.User;
import com.example.joinUs.model.neo4j.UserNeo4J;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(config = CentralMappingConfig.class, uses = { CityMapper.class,
        EventEmbeddedMapper.class, TopicEmbeddedMapper.class })
public interface UserMapper {

    @Mapping(target = "id", source = "id")
    UserDTO toDTO(User user);

    //    @Mapping(target = "roles",ignore = true)
    //    @Mapping(target = "authorities",ignore = true)
    //    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isAdmin", ignore = true)
    @Mapping(target = "id", source = "id")
    User toEntity(UserDTO dto);

    @Mapping(target = "id", source = "memberId")
    @Mapping(target = "memberName", source = "memberName")
    @Mapping(target = "city.name", source = "cityName")
    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "groupCount", ignore = true)
    @Mapping(target = "eventCount", ignore = true)
    @Mapping(target = "upcomingEvents", ignore = true)
    UserDTO toNeo4jDTO(UserNeo4J user);

    @Mapping(source = "id", target = "memberId")
    @Mapping(source = "memberName", target = "memberName")
    @Mapping(source = "city.name", target = "cityName")
    UserNeo4J toNeo4jEntity(UserDTO dto);

    //
    /*
        @Mapping(target = "topics", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "groupCount", ignore = true)
    @Mapping(target = "eventCount", ignore = true)
    @Mapping(target = "upcomingEvents", ignore = true)

     */

}
