package com.example.arimaabackend.controllers;

import com.example.arimaabackend.dto.MatchResponse;
import com.example.arimaabackend.services.MatchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Integer id) {
        return matchService.getMatch(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse createMatch(@RequestBody String matchData) {
        return matchService.createMatch(matchData);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse updateMatch(@PathVariable Integer id, @RequestBody String matchData) {
        return matchService.updateMatch(id, matchData);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMatch(@PathVariable Integer id) {
        matchService.deleteMatch(id);
    }
}
