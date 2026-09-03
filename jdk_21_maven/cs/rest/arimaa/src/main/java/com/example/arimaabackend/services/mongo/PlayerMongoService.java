package com.example.arimaabackend.services.mongo;

import java.util.List;

import com.example.arimaabackend.dto.PlayerCreateRequest;
import com.example.arimaabackend.dto.PlayerResponse;
import com.example.arimaabackend.dto.PlayerUpdateRequest;

public interface PlayerMongoService {
    PlayerResponse create(PlayerCreateRequest request);
    PlayerResponse update(Integer id, PlayerUpdateRequest request);
    PlayerResponse getByUsername(String username);
    PlayerResponse getById(Integer id);
    List<PlayerResponse> getAll();
    List<PlayerResponse> getByCountryName(String countryName);
    void deleteById(Integer id);
}
