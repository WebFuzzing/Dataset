package com.example.arimaabackend.services.neo4j;

import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserResponse;
import com.example.arimaabackend.dto.UserUpdateRequest;

public interface UserNeo4jService {
    UserResponse create(UserCreateRequest request);
    UserResponse getById(Long id);
    UserResponse update(Long id, UserUpdateRequest request);
    void deleteById(Long id);
}
