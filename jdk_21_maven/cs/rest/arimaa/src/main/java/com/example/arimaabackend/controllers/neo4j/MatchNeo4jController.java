package com.example.arimaabackend.controllers.neo4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.arimaabackend.dto.MatchResponse;
import com.example.arimaabackend.services.neo4j.MatchNeo4jService;

@RestController
@RequestMapping("/api/neo4j/matches")
public class MatchNeo4jController {

    private final MatchNeo4jService matchNeo4jService;

    public MatchNeo4jController(MatchNeo4jService matchNeo4jService) {
        this.matchNeo4jService = matchNeo4jService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Integer id) {
        return matchNeo4jService.getMatch(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
