package com.example.arimaabackend.unittests;

import com.example.arimaabackend.dto.UserCreateRequest;
import com.example.arimaabackend.enums.UserRole;
import com.example.arimaabackend.model.sql.UserEntity;
import com.example.arimaabackend.repository.sql.UserJpaRepository;
import com.example.arimaabackend.services.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private UserEntity savedEntity;

    @BeforeEach
    void setUp() {
        savedEntity = new UserEntity();
        savedEntity.setUsername("alice");
        savedEntity.setEmail("alice@example.com");
        savedEntity.setPasswordHash("$2a$hashed");
        savedEntity.setRole(UserRole.USER);
        // simulate @PrePersist
        var now = Instant.now();
        try {
            var createdAt = UserEntity.class.getDeclaredField("createdAt");
            createdAt.setAccessible(true);
            createdAt.set(savedEntity, now);
            var updatedAt = UserEntity.class.getDeclaredField("updatedAt");
            updatedAt.setAccessible(true);
            updatedAt.set(savedEntity, now);
            var id = UserEntity.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(savedEntity, 1L);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void create_hashesPasswordAndDefaultsRoleToUser() {
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$hashed");
        when(userJpaRepository.save(any(UserEntity.class))).thenReturn(savedEntity);

        var request = new UserCreateRequest("alice", "alice@example.com", "secret123");
        var response = userService.create(request);

        assertThat(response.username()).isEqualTo("alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo("USER");
        verify(passwordEncoder).encode("secret123");
    }

    @Test
    void getById_returnsUser_whenFound() {
        when(userJpaRepository.findById(1L)).thenReturn(Optional.of(savedEntity));

        var response = userService.getById(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    void getById_throws404_whenNotFound() {
        when(userJpaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteById_deletesUser_whenFound() {
        when(userJpaRepository.existsById(1L)).thenReturn(true);

        userService.deleteById(1L);

        verify(userJpaRepository).deleteById(1L);
    }

    @Test
    void deleteById_throws404_whenNotFound() {
        when(userJpaRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> userService.deleteById(99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("99");

        verify(userJpaRepository, never()).deleteById(any());
    }
}
