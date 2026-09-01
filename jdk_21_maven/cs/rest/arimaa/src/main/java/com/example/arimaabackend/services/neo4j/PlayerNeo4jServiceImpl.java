package com.example.arimaabackend.services.neo4j;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.arimaabackend.dto.PlayerResponse;
import com.example.arimaabackend.model.neo4j.CountryNode;
import com.example.arimaabackend.model.neo4j.PlayerNode;
import com.example.arimaabackend.model.neo4j.UserNode;
import com.example.arimaabackend.repository.neo4j.PlayerNeo4jRepository;

@Service
public class PlayerNeo4jServiceImpl implements PlayerNeo4jService {

    private final PlayerNeo4jRepository playerNeo4jRepository;

    public PlayerNeo4jServiceImpl(PlayerNeo4jRepository playerNeo4jRepository) {
        this.playerNeo4jRepository = playerNeo4jRepository;
    }

    @Override
    public PlayerResponse getByUsername(String username) {
        return playerNeo4jRepository.findByUsername(username)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Player with username '%s' not found in Neo4j".formatted(username)));
    }

    @Override
    public List<PlayerResponse> getByCountryName(String countryName) {
        return playerNeo4jRepository.findByCountryName(countryName).stream()
                .map(this::toResponse)
                .toList();
    }

    private PlayerResponse toResponse(PlayerNode node) {
        UserNode user = node.getUser();
        CountryNode country = node.getCountry();
        return new PlayerResponse(
                node.getId(),
                user != null ? user.getUsername() : null,
                user != null ? user.getEmail() : null,
                node.getRating(),
                node.getRu(),
                node.getGamesPlayed(),
                null,
                country != null ? country.getId() : null
        );
    }
}
