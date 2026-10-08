package it.pagopa.pn.papertracker.middleware.dao.dynamo;

import software.amazon.awssdk.enhanced.dynamodb.AttributeConverter;
import software.amazon.awssdk.enhanced.dynamodb.AttributeValueType;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdditionalDetailsConverter implements AttributeConverter<Map<String, Object>> {

    @Override
    public AttributeValue transformFrom(Map<String, Object> value) {
        if (Objects.isNull(value)) {
            return AttributeValue.builder().nul(true).build();
        }
        return AttributeValue.builder().m(value.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> toAttr(e.getValue())
                        ))).build();
    }

    @Override
    public Map<String, Object> transformTo(AttributeValue attributeValue) {
        if (Boolean.TRUE.equals(attributeValue.nul())) {
            return null;
        }
        if (Objects.isNull(attributeValue.m())) {
            return Map.of();
        }
        return attributeValue.m().entrySet().stream().collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        e -> Optional.ofNullable(fromAttr(e.getValue())).orElse("")
                ));
    }

    @Override
    public AttributeValueType attributeValueType() {
        return AttributeValueType.M;
    }

    @Override
    public EnhancedType<Map<String, Object>> type() {
        return EnhancedType.mapOf(EnhancedType.of(String.class), EnhancedType.of(Object.class));
    }

    /* -------- conversioni -------- */

    private AttributeValue toAttr(Object v) {
        return switch (v) {
            case String s -> AttributeValue.builder().s(s).build();
            case Number n -> AttributeValue.builder().n(n.toString()).build();
            case Boolean b -> AttributeValue.builder().bool(b).build();
            case Map<?, ?> m -> AttributeValue.builder().m(m.entrySet().stream()
                            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> toAttr(e.getValue()))))
                    .build();
            case List<?> l -> AttributeValue.builder().l(l.stream().map(this::toAttr).toList()).build();
            case null -> AttributeValue.builder().nul(true).build();
            default -> throw new IllegalArgumentException("Unsupported type in additionalDetails: " + v.getClass());
        };
    }

    private Object fromAttr(AttributeValue attributeValue) {
        switch (attributeValue.type()) {
            case S -> {
                if (Objects.nonNull(attributeValue.s())) return attributeValue.s();
            }
            case N -> {
                if (Objects.nonNull(attributeValue.n())) return attributeValue.n();
            }
            case BOOL -> {
                if (Objects.nonNull(attributeValue.bool())) return attributeValue.bool();
            }
            case M -> {
                if (Objects.nonNull(attributeValue.m())) {
                    return attributeValue.m().entrySet().stream()
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> Optional.ofNullable(fromAttr(e.getValue())).orElse("")));
                }
            }
            case L -> {
                if (Objects.nonNull(attributeValue.l())) {
                    return attributeValue.l().stream()
                            .map(this::fromAttr)
                            .toList();
                }
            }
        }
        return null;
    }
}
