package com.example.demo.auth.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing the different types of tokens that can be used in authentication schemes.
 * Currently, only the "Bearer" token type is supported, which is commonly used in OAuth2 and JWT authentication.
 */
@Getter
@RequiredArgsConstructor
public enum TokenType {

    BEARER("Bearer");

    private final String value;

}
