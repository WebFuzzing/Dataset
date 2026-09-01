package com.example.arimaabackend.controllers.neo4j;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.arimaabackend.dto.PlayerResponse;
import com.example.arimaabackend.services.neo4j.PlayerNeo4jService;

@RestController
@RequestMapping("/api/neo4j/players")
public class PlayerNeo4jController {

    private final PlayerNeo4jService playerNeo4jService;

    public PlayerNeo4jController(PlayerNeo4jService playerNeo4jService) {
        this.playerNeo4jService = playerNeo4jService;
    }

    @GetMapping("/by-username/{username}")
    public PlayerResponse getByUsername(@PathVariable String username) {
        return playerNeo4jService.getByUsername(username);
    }

    @GetMapping("/by-country/{countryName}")
    public List<PlayerResponse> getByCountryName(@PathVariable String countryName) {
        return playerNeo4jService.getByCountryName(countryName);
    }
}
