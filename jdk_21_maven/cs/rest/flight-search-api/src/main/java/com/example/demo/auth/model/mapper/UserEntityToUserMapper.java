package com.example.demo.auth.model.mapper;

import com.example.demo.auth.model.User;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.common.model.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting {@link UserEntity} to {@link User}.
 * This interface extends {@link BaseMapper} and provides the mapping logic from the
 * {@link UserEntity} entity to a {@link User} DTO. It includes a method for mapping a
 * {@link UserEntity} to a {@link User}.
 */
@Mapper
public interface UserEntityToUserMapper extends BaseMapper<UserEntity, User> {

    /**
     * Maps a {@link UserEntity} to a {@link User}.
     *
     * @param source The {@link UserEntity} object to be mapped.
     * @return A {@link User} object containing the mapped data from the {@link UserEntity}.
     */
    @Override
    User map(UserEntity source);

    /**
     * Initializes and returns an instance of {@link UserEntityToUserMapper}.
     *
     * @return An initialized {@link UserEntityToUserMapper} instance.
     */
    static UserEntityToUserMapper initialize() {
        return Mappers.getMapper(UserEntityToUserMapper.class);
    }

}
