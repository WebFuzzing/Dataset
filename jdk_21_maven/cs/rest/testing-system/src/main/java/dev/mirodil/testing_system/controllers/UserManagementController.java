package dev.mirodil.testing_system.controllers;

import dev.mirodil.testing_system.dtos.PagedResponse;
import dev.mirodil.testing_system.dtos.UserCreateRequestDTO;
import dev.mirodil.testing_system.dtos.UserResponseDTO;
import dev.mirodil.testing_system.dtos.WrapResponseWithContentKey;
import dev.mirodil.testing_system.services.UserService;
import dev.mirodil.testing_system.utils.AppUtil;
import dev.mirodil.testing_system.utils.PageWithFilterRequest;
import dev.mirodil.testing_system.validations.ValidUserPageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class UserManagementController {
    private final UserService userService;

    public UserManagementController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('VIEW_PROFILE')")
    @GetMapping("/profile")
    public WrapResponseWithContentKey<?> getProfile() {
        UserResponseDTO currentUserDTO = userService.getProfile();
        return new WrapResponseWithContentKey<>(currentUserDTO);
    }

    @PreAuthorize("hasAuthority('MANAGE_ALL_USERS')")
    @GetMapping("/users")
    @PageableAsQueryParam
    @Operation(summary = "Get all users",
            description = "Retrieve a paginated list of users with optional sorting and filters (cannot add field in swagger-ui \uD83D\uDE05. So, recommend using my Postman collection).\n\nExample usage: users?page=0&size=7&sort=userId&email=info")
    public PagedResponse<UserResponseDTO> getAllUsers(
            @Parameter(hidden = true) @ValidUserPageRequest PageWithFilterRequest pageable,
            HttpServletRequest request
    ) {
        String fullUrl = AppUtil.getUrlWithQueryParams(request);
        return userService.getUsers(pageable, fullUrl);
    }

    @PreAuthorize("hasAuthority('MANAGE_ALL_USERS')")
    @GetMapping("/users/{id}")
    public WrapResponseWithContentKey<?> getUserById(@PathVariable Long id) {
        UserResponseDTO userDTO = userService.getUserById(id);
        return new WrapResponseWithContentKey<>(userDTO);
    }

    @PreAuthorize("hasAuthority('MANAGE_ALL_USERS')")
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequestDTO request) {
        UserResponseDTO userDTO = userService.createUser(request);
        return ResponseEntity.created(userDTO.path()).body(
                new WrapResponseWithContentKey<>(userDTO)
        );
    }
}
