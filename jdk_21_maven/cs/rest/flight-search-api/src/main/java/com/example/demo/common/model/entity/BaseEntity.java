package com.example.demo.common.model.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * A base entity class that provides common fields for tracking the creation
 * and update metadata of an entity. This class is intended to be extended by other
 * entity classes to include audit information such as creation and update timestamps
 * and user details.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseEntity {


    @Field(name = "createdAt")
    private LocalDateTime createdAt;

    @Field(name = "createdBy")
    private String createdBy;

    @Field(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Field(name = "updatedBy")
    private String updatedBy;


}
