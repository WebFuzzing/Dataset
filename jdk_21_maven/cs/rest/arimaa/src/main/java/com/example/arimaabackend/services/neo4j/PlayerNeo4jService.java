package com.example.arimaabackend.services.neo4j;

import java.util.List;

import com.example.arimaabackend.dto.PlayerResponse;

public interface PlayerNeo4jService {
    PlayerResponse getByUsername(String username);
    List<PlayerResponse> getByCountryName(String countryName);
}
