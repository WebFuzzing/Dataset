package com.example.demo.auth.model.entity;

import com.example.demo.common.model.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * Represents an entity for storing invalidated tokens in MongoDB.
 * Each token is uniquely identified by {@code tokenId}.
 * Extends {@link BaseEntity} for common timestamp fields.
 */
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "invalid-token-collection")
public class InvalidTokenEntity extends BaseEntity {

    @Id
    @Indexed(unique = true)
    private String id;

    @Field(name = "TOKEN_ID")
    private String tokenId;

}
