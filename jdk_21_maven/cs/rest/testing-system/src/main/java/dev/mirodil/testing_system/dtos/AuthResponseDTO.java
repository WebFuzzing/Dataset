package dev.mirodil.testing_system.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;


public record AuthResponseDTO(
        @JsonProperty("user")
        UserResponseDTO userDTO,
        @JsonProperty("token")
        Map<String, Object> jwtTokenDetails
) {
}
