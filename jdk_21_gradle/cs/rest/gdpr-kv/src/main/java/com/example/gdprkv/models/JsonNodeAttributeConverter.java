package com.example.gdprkv.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class JsonNodeAttributeConverter implements AttributeConverter<JsonNode> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public AttributeValue transformFrom(JsonNode input) {
        if (input == null || input.isNull()) {
            return AttributeValue.builder().nul(true).build();
        }
        try {
            return AttributeValue.builder().s(MAPPER.writeValueAsString(input)).build();
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize record value", e);
        }
    }

    @Override
    public JsonNode transformTo(AttributeValue attributeValue) {
        if (attributeValue == null) {
            return null;
        }
        if (Boolean.TRUE.equals(attributeValue.nul())) {
            return null;
        }
        String raw = attributeValue.s();
        if (raw == null) {
            return null;
        }
        try {
            return MAPPER.readTree(raw);
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid JSON stored in record value", e);
        }
    }

    @Override
    public EnhancedType<JsonNode> type() {
        return EnhancedType.of(JsonNode.class);
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.S;
    }
}
