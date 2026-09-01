package com.example.arimaabackend.unittests;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void encode_generatesHashForAdminAndUserPasswords() {
        String adminRawPassword = "adminpassword1";
        String userRawPassword = "userpassword1";

        String adminHash = passwordEncoder.encode(adminRawPassword);
        String userHash = passwordEncoder.encode(userRawPassword);

        assertThat(adminHash).startsWith("$2a$");
        assertThat(userHash).startsWith("$2a$");

        assertThat(passwordEncoder.matches(adminRawPassword, adminHash)).isTrue();
        assertThat(passwordEncoder.matches(userRawPassword, userHash)).isTrue();
    }

    @Test
    void encode_producesHashWithBCryptPrefix() {
        String hash = passwordEncoder.encode("secret123");

        assertThat(hash).startsWith("$2a$");
    }

    @Test
    void encode_doesNotReturnPlaintext() {
        String raw = "secret123";
        String hash = passwordEncoder.encode(raw);

        assertThat(hash).isNotEqualTo(raw);
    }

    @Test
    void matches_returnsTrueForCorrectPassword() {
        String hash = passwordEncoder.encode("secret123");

        assertThat(passwordEncoder.matches("secret123", hash)).isTrue();
    }

    @Test
    void matches_returnsFalseForWrongPassword() {
        String hash = passwordEncoder.encode("secret123");

        assertThat(passwordEncoder.matches("wrongPassword", hash)).isFalse();
    }

    @Test
    void encode_producesDifferentHashesForSamePassword() {
        String hash1 = passwordEncoder.encode("secret123");
        String hash2 = passwordEncoder.encode("secret123");

        assertThat(hash1).isNotEqualTo(hash2);
    }
}
