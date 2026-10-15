package com.example.demo.auth.security;

import com.example.demo.base.AbstractBaseServiceTest;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CustomAuthenticationEntryPointTest extends AbstractBaseServiceTest {

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private AuthenticationException authenticationException;

    @Test
    void shouldWriteCustomErrorResponseWhenAuthenticationFails() throws IOException {

        // Given
        CustomAuthenticationEntryPoint customAuthenticationEntryPoint = new CustomAuthenticationEntryPoint();

        // Mock behavior of HttpServletResponse
        ServletOutputStream mockOutputStream = mock(ServletOutputStream.class);
        when(httpServletResponse.getOutputStream()).thenReturn(mockOutputStream); // Mock getOutputStream()

        doNothing().when(httpServletResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);
        doNothing().when(httpServletResponse).setStatus(HttpStatus.UNAUTHORIZED.value());

        // When
        customAuthenticationEntryPoint.commence(httpServletRequest, httpServletResponse, authenticationException);

        // Then
        // Verify that setContentType is called with the correct content type
        verify(httpServletResponse).setContentType(MediaType.APPLICATION_JSON_VALUE);

        // Verify that setStatus is called with 401 Unauthorized status code
        verify(httpServletResponse).setStatus(HttpStatus.UNAUTHORIZED.value());

        // Verify that the output stream's write method is called with a non-empty response body
        verify(mockOutputStream).write(any(byte[].class));

    }

}