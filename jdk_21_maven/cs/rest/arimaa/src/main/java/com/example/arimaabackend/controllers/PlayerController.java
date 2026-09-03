package com.example.arimaabackend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.arimaabackend.dto.PlayerCreateRequest;
import com.example.arimaabackend.dto.PlayerResponse;
import com.example.arimaabackend.dto.PlayerUpdateRequest;
import com.example.arimaabackend.services.PlayerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PlayerResponse create(@Valid @RequestBody PlayerCreateRequest request) {
        return playerService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public PlayerResponse update(@PathVariable Integer id, @Valid @RequestBody PlayerUpdateRequest request) {
        return playerService.update(id, request);
    }

    @GetMapping()
    public List<PlayerResponse> getAll() {
        return playerService.getAll();
    }

    @GetMapping("/by-username/{username}")
    public PlayerResponse getByUsername(@PathVariable String username) {
        return playerService.getByUsername(username);
    }

    @GetMapping("/{id}")
    public PlayerResponse getById(@PathVariable Integer id) {
        return playerService.getById(id);
    }

    @GetMapping("/by-country/{countryName}")
    public List<PlayerResponse> getByCountryName(@PathVariable String countryName) {
        return playerService.getByCountryName(countryName);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(@PathVariable Integer id) {
        playerService.deleteById(id);
    }

}
