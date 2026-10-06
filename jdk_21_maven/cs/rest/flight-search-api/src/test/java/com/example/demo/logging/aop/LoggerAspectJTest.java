package com.example.demo.logging.aop;

import com.example.demo.auth.exception.*;
import com.example.demo.base.AbstractBaseServiceTest;
import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.exception.FlightNotFoundException;
import com.example.demo.logging.entity.LogEntity;
import com.example.demo.logging.service.LogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Test class for the {@link LoggerAspectJ}, verifying that logging aspects
 * are correctly applied to target methods and provide the expected output.
 */
class LoggerAspectJTest extends AbstractBaseServiceTest {

    @InjectMocks
    private LoggerAspectJ loggerAspectJ;

    @Mock
    private LogService logService;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private ServletRequestAttributes servletRequestAttributes;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    @BeforeEach
    public void setUp() {
        // Initialize mocks and set request attributes
        when(servletRequestAttributes.getRequest()).thenReturn(httpServletRequest);
        when(servletRequestAttributes.getResponse()).thenReturn(httpServletResponse);
        RequestContextHolder.setRequestAttributes(servletRequestAttributes);

        // Mock JoinPoint signature
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getName()).thenReturn("testMethod");
        when(signature.getDeclaringTypeName()).thenReturn("LoggerAspectJ");
        when(signature.getDeclaringType()).thenReturn(LoggerAspectJ.class);
    }

    @Test
    public void testLogAfterThrowing() {

        // Given
        Exception ex = new UserNotFoundException("User not found");

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testUser");
        SecurityContextHolder.setContext(securityContext);

        // Then
        loggerAspectJ.logAfterThrowing(joinPoint, ex);

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterThrowing_AnonymousUser() {

        // Given
        Exception ex = new UserNotFoundException("User not found");
        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_ANONYMOUS");
        List<GrantedAuthority> authorities = Collections.singletonList(authority);
        SecurityContextHolder.setContext(securityContext);

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(securityContext.getAuthentication()).thenReturn(new AnonymousAuthenticationToken("key", "anonymousUser", authorities));

        // Then
        loggerAspectJ.logAfterThrowing(joinPoint, ex);

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning() throws IOException {

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletResponse.getStatus()).thenReturn(HttpStatus.OK.value());
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getSignature()).thenReturn(signature);

        // Then
        loggerAspectJ.logAfterReturning(joinPoint, "test response");

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning_WithJsonNode() throws IOException {

        // Given
        JsonNode jsonNode = new ObjectMapper().createObjectNode().put("key", "value");

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletResponse.getStatus()).thenReturn(HttpStatus.OK.value());

        // Then
        loggerAspectJ.logAfterReturning(joinPoint, jsonNode);

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning_NoResponseAttributes() throws IOException {

        // Given
        RequestContextHolder.resetRequestAttributes();

        // When
        loggerAspectJ.logAfterReturning(mock(JoinPoint.class), "test response");

        // Then
        verify(logService, never()).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterThrowing_LogServiceException() {

        // Given
        Exception ex = new UserNotFoundException("User not found");

        // When
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("GET");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("testUser");
        SecurityContextHolder.setContext(securityContext);
        doThrow(new RuntimeException("Database connection error")).when(logService).saveLogToDatabase(any(LogEntity.class));

        // Then
        loggerAspectJ.logAfterThrowing(joinPoint, ex);

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testLogAfterReturning_SaveLogThrowsException() throws IOException {

        // Given
        when(httpServletRequest.getRequestURL()).thenReturn(new StringBuffer("http://localhost/api/test"));
        when(httpServletRequest.getMethod()).thenReturn("POST");
        when(httpServletResponse.getStatus()).thenReturn(HttpStatus.OK.value());
        when(signature.getName()).thenReturn("testMethod");
        when(joinPoint.getSignature()).thenReturn(signature);

        // When
        doThrow(new RuntimeException("Database error")).when(logService).saveLogToDatabase(any(LogEntity.class));

        // Then
        Assertions.assertDoesNotThrow(() -> loggerAspectJ.logAfterReturning(joinPoint, "test response"));

        // Verify
        verify(logService, times(1)).saveLogToDatabase(any(LogEntity.class));

    }

    @Test
    public void testGetHttpStatusFromException_AllCases() {

        // Given
        Map<Exception, String> testCases = Map.of(
                new PasswordNotValidException("Invalid password"), PasswordNotValidException.STATUS.name(),
                new RoleNotFoundException("Role not found"), RoleNotFoundException.STATUS.name(),
                new TokenAlreadyInvalidatedException("Token already invalidated"), TokenAlreadyInvalidatedException.STATUS.name(),
                new UserAlreadyExistException("User already exists"), UserAlreadyExistException.STATUS.name(),
                new UserNotFoundException("User not found"), UserNotFoundException.STATUS.name(),
                new UserStatusNotValidException("User status not valid"), UserStatusNotValidException.STATUS.name(),
                new AirportNotFoundException("Airport not found"), AirportNotFoundException.STATUS.name(),
                new AirportNameAlreadyExistException("Airport name is already exist"), AirportNameAlreadyExistException.STATUS.name(),
                new FlightNotFoundException("Airport not found"), FlightNotFoundException.STATUS.name(),
                new Exception("Unknown exception"), HttpStatus.INTERNAL_SERVER_ERROR.name()
        );

        // When & Then
        testCases.forEach((exception, expectedStatus) -> {
            String actualStatus = (String) ReflectionTestUtils.invokeMethod(
                    loggerAspectJ,
                    "getHttpStatusFromException",
                    exception
            );
            Assertions.assertEquals(expectedStatus, actualStatus,
                    "Failed for exception: " + exception.getClass().getSimpleName());
        });

    }

}