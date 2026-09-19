package dev.mirodil.testing_system.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import dev.mirodil.testing_system.controllers.TestTakerController;
import dev.mirodil.testing_system.controllers.UserManagementController;
import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.models.enums.PermissionType;
import dev.mirodil.testing_system.models.enums.UserGender;
import dev.mirodil.testing_system.models.enums.UserRole;
import dev.mirodil.testing_system.models.enums.UserStatus;
import dev.mirodil.testing_system.utils.AuthUtil;

import java.net.URI;
import java.time.Instant;
import java.util.Set;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponseDTO(
        Long userId,
        UserRole userRole,
        String email,
        UserGender gender,
        UserStatus status,
        String fname,
        String lname,
        Boolean isAccountNonLocked,
        Instant createdAt,
        URI path,
        Set<PermissionType> permissions
) {
    public UserResponseDTO(User user) {
        this(user, resolvePath(user));
    }

    public UserResponseDTO(User user, URI fullURL) {
        this(
                user.getId(),
                user.getUserRoleName(),
                user.getEmail(),
                user.getGender(),
                user.getStatus(),
                user.getFirstName(),
                user.getLastName(),
                user.isAccountNonLocked(),
                user.getCreatedAt(),
                fullURL,
                user.getPermissionNames()
        );
    }

    private static URI resolvePath(User user) throws RuntimeException {
        UserRole currentRole = UserRole.TEST_TAKER;
        if (AuthUtil.isUserAuthenticated()) {
            currentRole = AuthUtil.getAuthenticatedUserRole();
        }

        switch (currentRole) {
            case ADMIN -> {
                return linkTo(methodOn(UserManagementController.class).getUserById(user.getId())).toUri();
            }
            case TEST_TAKER -> {
                return linkTo(methodOn(TestTakerController.class).getProfile()).toUri();
            }
            default ->
                    throw new RuntimeException("Unexpected UserRole type: " + currentRole + ". Cannot resolve path.");
        }
    }
}
