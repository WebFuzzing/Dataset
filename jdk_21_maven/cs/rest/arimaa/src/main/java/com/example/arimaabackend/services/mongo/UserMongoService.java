package com.example.arimaabackend.services.mongo;

import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserResponse;
import com.example.arimaabackend.dto.UserUpdateRequest;

public interface UserMongoService {
    UserResponse create(UserCreateRequest request);
    UserResponse getById(Long id);
    UserResponse update(Long id, UserUpdateRequest request);
    UserResponse getByUsername(String username);
    void deleteById(Long id);
}
