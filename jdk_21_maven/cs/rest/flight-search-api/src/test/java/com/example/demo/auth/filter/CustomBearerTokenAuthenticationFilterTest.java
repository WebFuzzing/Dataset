package com.example.demo.auth.filter;

import com.example.demo.auth.service.InvalidTokenService;
import com.example.demo.auth.service.TokenService;
import com.example.demo.base.AbstractBaseServiceTest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomBearerTokenAuthenticationFilterTest extends AbstractBaseServiceTest {

    @InjectMocks
    private CustomBearerTokenAuthenticationFilter customBearerTokenAuthenticationFilter;

    @Mock
    private TokenService tokenService;

    @Mock
    private InvalidTokenService invalidTokenService;

    @BeforeEach
    void setup() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWithValidBearerToken() throws Exception {

        // Given
        String validJwt = "valid-jwt-token";
        String tokenId = "valid-token-id";
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // When
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + validJwt);
        when(tokenService.getId(validJwt)).thenReturn(tokenId);
        when(tokenService.getAuthentication(validJwt)).thenReturn(new UsernamePasswordAuthenticationToken("user", null));

        // Then
        customBearerTokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("user", SecurityContextHolder.getContext().getAuthentication().getName());

        // Verify
        verify(tokenService).verifyAndValidate(validJwt);
        verify(invalidTokenService).checkForInvalidityOfToken(tokenId);
        verify(tokenService).getAuthentication(validJwt);
        verify(filterChain).doFilter(request, response);

    }

    @Test
    void shouldNotAuthenticateWithoutBearerToken() throws Exception {

        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);

        // When
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // Then
        customBearerTokenAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());

        // Verify
        verify(tokenService, never()).verifyAndValidate(anyString());
        verify(invalidTokenService, never()).checkForInvalidityOfToken(any());
        verify(tokenService, never()).getAuthentication(any());
        verify(filterChain).doFilter(request, response);

    }

}