package com.example.arimaabackend.services.mongo;

import com.example.arimaabackend.dto.MatchResponse;
import java.util.Optional;

public interface MatchMongoService {
    Optional<MatchResponse> getMatch(Integer id);
    MatchResponse createMatch(String matchData);
    void deleteMatch(Integer id);
    MatchResponse updateMatch(Integer id, String matchData);
}
