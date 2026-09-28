package com.example.gdprkv.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class JsonStringMapAttributeConverter implements AttributeConverter<Map<String, Object>> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(Map<String, Object> input) {
        String json = toJsonString(input);
        return AttributeValue.builder().s(json).build();
    }

    @Override
    public Map<String, Object> transformTo(AttributeValue attributeValue) {
        String json = attributeValue.s();
        try {
            return MAPPER.readValue(
                    json,
                    MAPPER.getTypeFactory().constructMapType(Map.class, String.class, Object.class)
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON in details attribute", e);
        }
    }

    @Override
    public EnhancedType<Map<String, Object>> type() {
        return EnhancedType.mapOf(String.class, Object.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S; // stored as a JSON string
    }

    static String toJsonString(Map<String, Object> input) {
        try {
            return MAPPER.writeValueAsString(input == null ? Map.of() : input);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize details map", e);
        }
    }
}
