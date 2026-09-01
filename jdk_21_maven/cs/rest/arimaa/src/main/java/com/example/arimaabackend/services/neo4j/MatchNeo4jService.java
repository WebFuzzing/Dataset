package com.example.arimaabackend.services.neo4j;

import java.util.Optional;

import com.example.arimaabackend.dto.MatchResponse;

public interface MatchNeo4jService {
    Optional<MatchResponse> getMatch(Integer id);
}
