package com.example.demo.auth.model.mapper;

import com.example.demo.auth.model.dto.request.RegisterRequest;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.common.model.mapper.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/**
 * Mapper interface for converting {@link RegisterRequest} to {@link UserEntity}.
 * This interface extends {@link BaseMapper} and provides the mapping logic from the
 * {@link RegisterRequest} DTO to a {@link UserEntity} entity. It includes a custom mapping method
 * for saving the {@link RegisterRequest} to the {@link UserEntity}.
 */
@Mapper
public interface RegisterRequestToUserEntityMapper extends BaseMapper<RegisterRequest, UserEntity> {

    /**
     * Maps a {@link RegisterRequest} to a {@link UserEntity} for saving purposes.
     *
     * @param registerRequest The {@link RegisterRequest} object to be mapped.
     * @return A {@link UserEntity} populated with the data from the given {@link RegisterRequest}.
     */
    @Named("mapForSaving")
    default UserEntity mapForSaving(RegisterRequest registerRequest) {
        return UserEntity.builder()
                .email(registerRequest.getEmail())
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .phoneNumber(registerRequest.getPhoneNumber())
                .userType(registerRequest.getUserType())
                .build();
    }

    /**
     * Initializes and returns an instance of {@link RegisterRequestToUserEntityMapper}.
     *
     * @return An initialized {@link RegisterRequestToUserEntityMapper} instance.
     */
    static RegisterRequestToUserEntityMapper initialize() {
        return Mappers.getMapper(RegisterRequestToUserEntityMapper.class);
    }

}
