package com.example.arimaabackend.controllers.mongo;

import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.dto.UserResponse;
import com.example.arimaabackend.dto.UserUpdateRequest;
import com.example.arimaabackend.services.mongo.UserMongoService;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/mongo/users")
public class UserMongoController {

    private final UserMongoService userMongoService;

    public UserMongoController(UserMongoService userMongoService) {
        this.userMongoService = userMongoService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userMongoService.create(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getById(@PathVariable Long id) {
        return userMongoService.getById(id);
    }

    @GetMapping("/by-username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getByUsername(@PathVariable String username) {
        return userMongoService.getByUsername(username);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userMongoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(@PathVariable Long id) {
        userMongoService.deleteById(id);
    }
}
