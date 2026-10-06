package com.example.demo.common.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * A generic class that represents a standard response structure.
 * It includes metadata about the response such as the status, success flag,
 * response data, and timestamp.
 *
 * @param <T> The type of the response data.
 */
@Getter
@Builder
public class CustomResponse<T> {

    @Builder.Default
    private LocalDateTime time = LocalDateTime.now();

    private HttpStatus httpStatus;

    private Boolean isSuccess;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T response;

    /**
     * A predefined successful response with no data. It represents a successful
     * operation with a HTTP status of 200 (OK).
     */
    public static final CustomResponse<Void> SUCCESS = CustomResponse.<Void>builder()
            .httpStatus(HttpStatus.OK)
            .isSuccess(true)
            .build();


    /**
     * Creates a successful response with the provided data.
     *
     * @param response The response data to include in the response body.
     * @param <T> The type of the response data.
     * @return A {@link CustomResponse} instance with the provided response data.
     */
    public static <T> CustomResponse<T> successOf(final T response) {
        return CustomResponse.<T>builder()
                .httpStatus(HttpStatus.OK)
                .isSuccess(true)
                .response(response)
                .build();
    }

}
