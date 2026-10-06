package com.example.demo.logging.aop;

import com.example.demo.auth.exception.*;
import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.exception.AirportNotFoundException;
import com.example.demo.flight.exception.FlightNotFoundException;
import com.example.demo.logging.entity.LogEntity;
import com.example.demo.logging.service.LogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.util.Optional;

/**
 * Aspect for logging method execution and exceptions in REST controllers.
 * This class intercepts all method calls and exception throws in any class annotated with {@link org.springframework.web.bind.annotation.RestController}.
 * It logs information about the HTTP request, response, and any exceptions that are thrown.
 * The logs are saved to the database using the {@link LogService}.
 */
@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggerAspectJ {

    private final LogService logService;

    /**
     * Pointcut that matches methods within classes annotated with {@link org.springframework.web.bind.annotation.RestController}.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void restControllerPointcut() {

    }

    /**
     * After throwing advice that logs exception details when an exception is thrown from a REST controller method.
     * This method logs details such as the HTTP request URL, HTTP method, exception message, and the operation that threw the exception.
     * It also retrieves the username from the {@link SecurityContextHolder} if available and logs it.
     *
     * @param joinPoint The join point representing the method execution.
     * @param ex       The exception thrown by the method.
     */
    @AfterThrowing(pointcut = "restControllerPointcut()", throwing = "ex")
    public void logAfterThrowing(JoinPoint joinPoint, Exception ex) {

        Optional<ServletRequestAttributes> requestAttributes = Optional.ofNullable(
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes()
        );

        if (requestAttributes.isPresent()) {

            final HttpServletRequest request = requestAttributes.get().getRequest();

            LogEntity logEntity = LogEntity.builder()
                    .endpoint(request.getRequestURL().toString())
                    .method(request.getMethod())
                    .message(ex.getMessage())
                    .errorType(ex.getClass().getName())
                    .status(getHttpStatusFromException(ex))
                    .operation(joinPoint.getSignature().getName())
                    .response(ex.getMessage())
                    .build();

            // Get the username from SecurityContextHolder and set it in logEntity
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && !(authentication instanceof AnonymousAuthenticationToken)) {
                String username = authentication.getName();
                logEntity.setUserInfo(username);
            }

            try {
                logService.saveLogToDatabase(logEntity);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else {
            log.error("Request Attributes are null!");
        }

    }

    /**
     * After returning advice that logs response details when a REST controller method successfully returns a result.
     * This method logs information such as the HTTP request URL, HTTP method, operation, and response message.
     * It also includes the HTTP status code in the log.
     *
     * @param joinPoint The join point representing the method execution.
     * @param result    The result returned by the method.
     * @throws IOException If there is an issue serializing the response object.
     */
    @AfterReturning(value = "restControllerPointcut()", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Object result) throws IOException {

        Optional<ServletRequestAttributes> requestAttributes = Optional.ofNullable(
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes()
        );

        if (requestAttributes.isPresent()) {

            final HttpServletRequest request = requestAttributes.get().getRequest();
            final HttpServletResponse response = requestAttributes.get().getResponse();

            String responseObject = "";

            LogEntity logEntity = LogEntity.builder()
                    .endpoint(request.getRequestURL().toString())
                    .method(request.getMethod())
                    .operation(joinPoint.getSignature().getName())
                    .build();

            if (result instanceof JsonNode) {
                ObjectMapper objectMapper = new ObjectMapper();
                responseObject = objectMapper.writeValueAsString(result);
            } else {
                responseObject = result.toString();
            }

            logEntity.setResponse(responseObject);
            logEntity.setMessage(responseObject);
            Optional.ofNullable(response).ifPresent(
                    httpServletResponse -> logEntity.setStatus(
                            String.valueOf(HttpStatus.valueOf(httpServletResponse.getStatus()
                            ))
                    ));

            try {
                logService.saveLogToDatabase(logEntity);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

        } else {
            log.error("Request Attributes are null!");
        }
    }

    /**
     * Retrieves the HTTP status from the exception type.
     * This method maps specific exception classes to their corresponding HTTP status values.
     *
     * @param ex The exception thrown.
     * @return The HTTP status as a string corresponding to the exception type.
     */
    private String getHttpStatusFromException(Exception ex) {
        return switch (ex.getClass().getSimpleName()) {
            case "PasswordNotValidException" -> PasswordNotValidException.STATUS.name();
            case "RoleNotFoundException" -> RoleNotFoundException.STATUS.name();
            case "TokenAlreadyInvalidatedException" -> TokenAlreadyInvalidatedException.STATUS.name();
            case "UserAlreadyExistException" -> UserAlreadyExistException.STATUS.name();
            case "UserNotFoundException" -> UserNotFoundException.STATUS.name();
            case "UserStatusNotValidException" -> UserStatusNotValidException.STATUS.name();
            case "AirportNotFoundException" -> AirportNotFoundException.STATUS.name();
            case "AirportNameAlreadyExistException" -> AirportNameAlreadyExistException.STATUS.name();
            case "FlightNotFoundException" -> FlightNotFoundException.STATUS.name();
            default -> HttpStatus.INTERNAL_SERVER_ERROR.name();
        };
    }

}
