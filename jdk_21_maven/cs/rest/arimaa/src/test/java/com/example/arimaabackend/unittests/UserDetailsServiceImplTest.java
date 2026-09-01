package com.example.arimaabackend.unittests;

import com.example.arimaabackend.enums.UserRole;
import com.example.arimaabackend.model.sql.UserEntity;
import com.example.arimaabackend.repository.sql.UserJpaRepository;
import com.example.arimaabackend.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserJpaRepository userJpaRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsername_returnsUserDetails_withUserRole() {
        var entity = new UserEntity();
        entity.setUsername("alice");
        entity.setPasswordHash("$2a$hashed");
        entity.setRole(UserRole.USER);
        when(userJpaRepository.findByUsername("alice")).thenReturn(Optional.of(entity));

        var details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("$2a$hashed");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }

    @Test
    void loadUserByUsername_returnsUserDetails_withAdminRole() {
        var entity = new UserEntity();
        entity.setUsername("bob");
        entity.setPasswordHash("$2a$adminhash");
        entity.setRole(UserRole.ADMIN);
        when(userJpaRepository.findByUsername("bob")).thenReturn(Optional.of(entity));

        var details = userDetailsService.loadUserByUsername("bob");

        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_throwsUsernameNotFoundException_whenNotFound() {
        when(userJpaRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
