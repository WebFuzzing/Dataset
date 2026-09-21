package dev.mirodil.testing_system.models.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import dev.mirodil.testing_system.exceptions.GenericValidationError;

import java.io.IOException;
import java.util.Arrays;

public class UserRoleDeserializer extends JsonDeserializer<UserRole> {
    @Override
    public UserRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase();
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new GenericValidationError("Invalid role value: " + value + ". Supported values: " + Arrays.toString(UserRole.values()));
        }
    }
}