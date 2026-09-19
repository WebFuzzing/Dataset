package dev.mirodil.testing_system.controllers;

import dev.mirodil.testing_system.dtos.AuthResponseDTO;
import dev.mirodil.testing_system.dtos.TestTakerRegisterRequestDTO;
import dev.mirodil.testing_system.dtos.UserResponseDTO;
import dev.mirodil.testing_system.dtos.WrapResponseWithContentKey;
import dev.mirodil.testing_system.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@RestController
@RequestMapping("/test-taker")
public class TestTakerController {
    private final UserService userService;

    public TestTakerController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody TestTakerRegisterRequestDTO request, HttpServletRequest servletRequest) {
        AuthResponseDTO authResponseDTO = userService.createTestTaker(request, servletRequest);
        URI profilePath = linkTo(TestTakerController.class).withRel("profile").toUri();
        return ResponseEntity.created(profilePath).body(new WrapResponseWithContentKey<>(authResponseDTO));
    }

    @PreAuthorize("hasAuthority('VIEW_PROFILE')")
    @GetMapping("/profile")
    public WrapResponseWithContentKey<?> getProfile() {
        UserResponseDTO currentUserDTO = userService.getProfile();
        return new WrapResponseWithContentKey<>(currentUserDTO);
    }
}
