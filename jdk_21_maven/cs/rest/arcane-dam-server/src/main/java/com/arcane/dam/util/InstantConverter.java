package com.arcane.dam.util;

// MODIFIED: removed AWS SDK v1 DynamoDBTypeConverter dependency (SDK v2 natively supports Instant)
import java.time.Instant;

public class InstantConverter {
    public String convert(Instant instant) {
        return instant.toString();
    }

    public Instant unconvert(String value) {
        return Instant.parse(value);
    }
}
