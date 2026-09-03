package com.example.arimaabackend.controllers.mongo;

import com.example.arimaabackend.dto.MatchResponse;
import com.example.arimaabackend.services.mongo.MatchMongoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mongo/matches")
public class MatchMongoController {

    private final MatchMongoService matchMongoService;

    public MatchMongoController(MatchMongoService matchMongoService) {
        this.matchMongoService = matchMongoService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponse> getMatch(@PathVariable Integer id) {
        return matchMongoService.getMatch(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse createMatch(@RequestBody String matchData) {
        return matchMongoService.createMatch(matchData);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public MatchResponse updateMatch(@PathVariable Integer id, @RequestBody String matchData) {
        return matchMongoService.updateMatch(id, matchData);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteMatch(@PathVariable Integer id) {
        matchMongoService.deleteMatch(id);
    }
}
