package com.example.demo.common.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

/**
 * A base class for domain models that include common auditing fields such as
 * creation and update timestamps, as well as the user responsible for those changes.
 * This class serves as a foundation for other domain models, providing essential fields
 * to track the creation and modification metadata, which is crucial for auditing purposes.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseDomainModel {

    @Field(name = "createdAt")
    private LocalDateTime createdAt;

    @Field(name = "createdBy")
    private String createdBy;

    @Field(name = "updatedAt")
    private LocalDateTime updatedAt;

    @Field(name = "updatedBy")
    private String updatedBy;
}
