package it.pagopa.pn.mandate.middleware.db;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import lombok.*;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_UNSUPPORTED_LAST_EVALUTED_KEY;

@Getter
@Setter
@ToString
public class PnLastEvaluatedKey {

    private static final ObjectWriter writer = new ObjectMapper().writerFor(KeyPair.class);
    private static final ObjectReader reader = new ObjectMapper().readerFor(KeyPair.class);

    private String externalLastEvaluatedKey;
    private Map<String, AttributeValue> internalLastEvaluatedKey;

    public static PnLastEvaluatedKey deserialize(String encoded) {
        String json = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        try {
            KeyPair keyPair = reader.readValue(json);
            PnLastEvaluatedKey pnLastEvaluatedKey = new PnLastEvaluatedKey();
            pnLastEvaluatedKey.setExternalLastEvaluatedKey(keyPair.getEk());
            pnLastEvaluatedKey.setInternalLastEvaluatedKey(keyPair.ikToDynamo());
            return pnLastEvaluatedKey;
        } catch (JsonProcessingException e) {
            throw new PnInternalException("Unable to deserialize LastEvaluatedKey", ERROR_CODE_MANDATE_UNSUPPORTED_LAST_EVALUTED_KEY, e);
        }
    }

    public String serialize() {
        Map<String, String> internalValues = new HashMap<>();
        internalLastEvaluatedKey.forEach((k, v) -> internalValues.put(k, v.s() != null ? v.s() : v.n()));
        KeyPair toSerialize = new KeyPair(externalLastEvaluatedKey, internalValues);
        try {
            String result = writer.writeValueAsString(toSerialize);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            throw new PnInternalException("Unable to serialize LastEvaluatedKey", ERROR_CODE_MANDATE_UNSUPPORTED_LAST_EVALUTED_KEY, e);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KeyPair {

        private String ek;
        private Map<String, String> ik;

        private Map<String, AttributeValue> ikToDynamo() {
            Map<String, AttributeValue> result = new HashMap<>();
            for (Map.Entry<String, String> entry : ik.entrySet()) {
                result.put(entry.getKey(), AttributeValue.builder().s(entry.getValue()).build());
            }
            return result;
        }
    }
}
