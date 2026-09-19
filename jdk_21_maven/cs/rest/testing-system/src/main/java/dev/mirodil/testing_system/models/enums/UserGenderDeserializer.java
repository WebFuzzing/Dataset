package dev.mirodil.testing_system.models.enums;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import dev.mirodil.testing_system.exceptions.GenericValidationError;

import java.io.IOException;
import java.util.Arrays;

public class UserGenderDeserializer extends JsonDeserializer<UserGender> {
    @Override
    public UserGender deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().toUpperCase();
        try {
            return UserGender.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new GenericValidationError("Invalid gender value: " + value + ". Supported values: " + Arrays.toString(UserGender.values()));
        }
    }
}