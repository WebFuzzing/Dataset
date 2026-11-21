package it.pagopa.pn.mandate.middleware.db;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PnLastEvaluatedKeyTest {

    @Test
    void testDeserializeAndSerialize() {
        PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey("external");
        lastEvaluatedKey.setInternalLastEvaluatedKey(Map.of("k", AttributeValue.builder().s("v").build()));

        String serialized = lastEvaluatedKey.serialize();
        PnLastEvaluatedKey deserialized = PnLastEvaluatedKey.deserialize(serialized);

        assertEquals(lastEvaluatedKey.getExternalLastEvaluatedKey(), deserialized.getExternalLastEvaluatedKey());
        assertEquals(lastEvaluatedKey.getInternalLastEvaluatedKey(), deserialized.getInternalLastEvaluatedKey());
    }
}