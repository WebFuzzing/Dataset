package dev.mirodil.testing_system.models.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = UserRoleDeserializer.class)
public enum UserRole {
    TEST_TAKER,
    ADMIN,
}
