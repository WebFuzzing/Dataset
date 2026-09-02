package com.example.joinUs.mapping.embedded;

import com.example.joinUs.mapping.CentralMappingConfig;
import com.example.joinUs.mapping.UserMapper;
import com.example.joinUs.model.embedded.UserEmbedded;
import com.example.joinUs.model.mongodb.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;


@Mapper(config = CentralMappingConfig.class, uses = { UserMapper.class })
public interface UserEmbeddedMapper {

    @Mapping(target = "memberId", source = "id")
    @Mapping(target = "memberName", source = "memberName")
    UserEmbedded toDTO(User user);

    @Mapping(target = "memberId", source = "id")
    @Mapping(target = "memberName", source = "memberName")
    List<UserEmbedded> toDTO(List<User> user);

    @Mapping(target = "topics", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "city", ignore = true)
    @Mapping(target = "groupCount", ignore = true)
    @Mapping(target = "eventCount", ignore = true)
    @Mapping(target = "upcomingEvents", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isAdmin", ignore = true)
    @Mapping(target = "id", source = "memberId")
    User toEntity(UserEmbedded userEmbedded);
}
