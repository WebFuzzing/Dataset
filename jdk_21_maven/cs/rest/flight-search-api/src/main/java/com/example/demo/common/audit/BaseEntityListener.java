package com.example.demo.common.audit;

import com.example.demo.auth.model.enums.TokenClaims;
import com.example.demo.common.model.entity.BaseEntity;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Listener for MongoDB lifecycle events to manage auditing fields in {@link BaseEntity}.
 */
@Component
public class BaseEntityListener extends AbstractMongoEventListener<BaseEntity> {

    /**
     * Sets auditing fields before converting the entity to a MongoDB document.
     *
     * @param event the event triggered before entity conversion.
     */
    @Override
    public void onBeforeConvert(BeforeConvertEvent<BaseEntity> event) {
        BaseEntity entity = event.getSource();

        // Set ID for entities that have it
        setEntityIdIfNeeded(entity);

        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
            entity.setCreatedBy(getCurrentUser());
        }

        // Always update 'updatedAt' and 'updatedBy'
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(getCurrentUser());

    }


    /**
     * Checks if the entity has an 'id' field and sets it to a new UUID if it is null or empty.
     *
     * @param entity the entity to check and modify
     */
    private void setEntityIdIfNeeded(BaseEntity entity) {
        try {
            // Use reflection to check if the entity has an 'id' field
            Field idField = entity.getClass().getDeclaredField("id");
            idField.setAccessible(true);  // Make the field accessible

            // If the field is null or empty, set it to a new UUID
            Object idValue = idField.get(entity);
            if (idValue == null || idValue.toString().isEmpty()) {
                idField.set(entity, UUID.randomUUID().toString());
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If no 'id' field is found, or it can't be accessed, ignore (entity may not have an 'id' field)
        }
    }

    /**
     * Retrieves the email of the currently authenticated user from the security context.
     * If no authenticated user is found, it returns "anonymousUser".
     *
     * @return the email of the current user or "anonymousUser" if not authenticated
     */
    private String getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getPrincipal)
                .filter(user -> !"anonymousUser".equals(user))
                .map(Jwt.class::cast)
                .map(jwt -> jwt.getClaim(TokenClaims.USER_EMAIL.getValue()).toString())
                .orElse("anonymousUser");
    }

}
