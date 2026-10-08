package it.pagopa.pn.papertracker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
@RequiredArgsConstructor
public class LogUtility {

    private static final String MASK = "***";

    private final ObjectMapper objectMapper;


    /**
     * Maschera i campi sensibili di un oggetto Java.
     *
     * @param obj        oggetto da loggare; se {@code null} viene restituita la stringa {@code "null"}
     * @param fieldNames nomi dei campi da mascherare (case-sensitive)
     * @return rappresentazione JSON con i campi sensibili mascherati
     */
    public String maskSensitiveData(Object obj, String... fieldNames) {
        if (obj == null) {
            return "null";
        }

        try {
            JsonNode rootNode = objectMapper.valueToTree(obj);
            maskSensitiveData(rootNode, fieldNames);
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            return obj.toString();
        }
    }

    /**
     * Maschera i campi sensibili di una stringa JSON.
     *
     * @param json       stringa JSON da loggare; se {@code null} viene restituita la stringa {@code "null"}
     * @param fieldNames nomi dei campi da mascherare (case-sensitive)
     * @return stringa JSON con i campi sensibili mascherati
     */
    public String maskSensitiveDataFromJson(String json, String... fieldNames) {
        if (json == null) {
            return "null";
        }

        try {
            JsonNode rootNode = objectMapper.readTree(json);
            maskSensitiveData(rootNode, fieldNames);
            return objectMapper.writeValueAsString(rootNode);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * Maschera i campi sensibili direttamente su un {@link JsonNode}.
     * <p>
     * Nessuna serializzazione o deserializzazione viene eseguita.
     * </p>
     *
     * @param rootNode   nodo JSON da mascherare
     * @param fieldNames nomi dei campi da mascherare (case-sensitive)
     * @return lo stesso {@link JsonNode} modificato
     */
    public JsonNode maskSensitiveData(JsonNode rootNode, String... fieldNames) {
        if (rootNode == null) {
            return null;
        }

        Set<String> fieldsToMask = new HashSet<>(Arrays.asList(fieldNames));
        maskRecursively(rootNode, fieldsToMask);
        return rootNode;
    }


    /**
     * Visita ricorsivamente un {@link JsonNode} e maschera i campi
     * il cui nome è presente nell'insieme {@code fieldsToMask}.
     *
     * @param node         nodo corrente dell'albero JSON
     * @param fieldsToMask insieme dei nomi dei campi da mascherare
     */
    private void maskRecursively(JsonNode node, Set<String> fieldsToMask) {

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;

            objectNode.fieldNames().forEachRemaining(fieldName -> {
                JsonNode child = objectNode.get(fieldName);

                if (fieldsToMask.contains(fieldName)) {
                    objectNode.set(fieldName, objectMapper.getNodeFactory().textNode(MASK));
                } else {
                    maskRecursively(child, fieldsToMask);
                }
            });

        } else if (node.isArray()) {
            for (JsonNode element : node) {
                maskRecursively(element, fieldsToMask);
            }
        }
    }

    /**
     * Converte un {@link Message} in una stringa pronta per il logging, anonimizzando
     * i campi sensibili del payload e rimuovendo l'header "Body".
     *
     * @param <T> tipo del payload del messaggio
     * @param message messaggio da convertire in stringa; non deve essere {@code null}
     * @param fieldsToMask insieme dei nomi dei campi del payload da mascherare; se vuoto o {@code null}, il payload non viene modificato
     * @return rappresentazione in stringa del messaggio con payload mascherato e headers senza "Body"
     */
    public <T> String messageToString(Message<T> message, Set<String> fieldsToMask) {
        T payload = message.getPayload();
        // Maschera il payload reale
        String maskedPayload = maskSensitiveData(payload, fieldsToMask.toArray(new String[0]));

        // Toglie Body da headers
        Map<String, Object> headersMap = new LinkedHashMap<>();
        message.getHeaders().forEach((k, v) -> {
            if (!SqsHeaders.SQS_SOURCE_DATA_HEADER.equalsIgnoreCase(k)) {
                headersMap.put(k, v);
            }
        });

        return "Message [payload=" + maskedPayload + ", headers=" + headersMap + "]";
    }
}