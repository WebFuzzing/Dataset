package com.example.demo.auth.model.entity;

import com.example.demo.auth.model.enums.TokenClaims;
import com.example.demo.auth.model.enums.UserStatus;
import com.example.demo.auth.model.enums.UserType;
import com.example.demo.common.model.entity.BaseEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user entity in the system, storing personal and authentication details.
 * Mapped to the MongoDB {@code user-collection}, with {@code id} as the unique identifier.
 * Extends {@link BaseEntity}, inheriting common properties like timestamps.
 */
@Getter
@Setter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "user-collection")
public class UserEntity extends BaseEntity {

    @Id
    @Indexed(unique = true)
    private String id;

    @Field(name = "EMAIL")
    @Indexed(unique = true)
    private String email;

    @Field(name = "PASSWORD")
    private String password;

    @Field(name = "FIRST_NAME")
    private String firstName;

    @Field(name = "LAST_NAME")
    private String lastName;

    @Field(name = "PHONE_NUMBER")
    private String phoneNumber;

    @Field(name = "USER_TYPE")
    private UserType userType;

    @Builder.Default
    @Field(name = "USER_STATUS")
    private UserStatus userStatus = UserStatus.ACTIVE;

    /**
     * Generates a map of claims associated with the user.
     * This method creates a map of key-value pairs representing the user's claims,
     * which can be used for JWT token generation or other purposes where user-related
     * data needs to be included.
     *
     * @return a map containing the user's claims.
     */
    public Map<String, Object> getClaims() {
        final Map<String, Object> claims = new HashMap<>();

        claims.put(TokenClaims.USER_ID.getValue(), this.id);
        claims.put(TokenClaims.USER_TYPE.getValue(), this.userType);
        claims.put(TokenClaims.USER_STATUS.getValue(), this.userStatus);
        claims.put(TokenClaims.USER_FIRST_NAME.getValue(), this.firstName);
        claims.put(TokenClaims.USER_LAST_NAME.getValue(), this.lastName);
        claims.put(TokenClaims.USER_EMAIL.getValue(), this.email);
        claims.put(TokenClaims.USER_PHONE_NUMBER.getValue(), this.phoneNumber);

        return claims;
    }

}
