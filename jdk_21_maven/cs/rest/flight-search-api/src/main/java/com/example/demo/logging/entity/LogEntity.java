package com.example.demo.logging.entity;

import com.example.demo.common.model.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * Represents a log entity stored in the MongoDB {@code log-collection}.
 * Captures details such as {@code message}, {@code endpoint}, {@code status}, and {@code time}.
 * Extends {@link BaseEntity} for common timestamp fields.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Document(collection = "log-collection")
public class LogEntity extends BaseEntity {

    @Id
    @Indexed(unique = true)
    private String id;

    @Field
    private String message;

    @Field
    private String endpoint;

    @Field
    private String method;

    @Field
    private String status;

    @Field
    private String userInfo;

    @Field
    private String errorType;

    @Field
    private String response;

    @Field
    private String operation;

    @Field
    private LocalDateTime time;
}
