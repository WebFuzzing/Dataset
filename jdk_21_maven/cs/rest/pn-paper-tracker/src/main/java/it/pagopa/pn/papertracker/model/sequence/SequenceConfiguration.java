package it.pagopa.pn.papertracker.model.sequence;

import java.util.*;
import java.util.stream.Collectors;

public class SequenceConfiguration {

    private static final Map<String, SequenceConfig> CONFIGS = new HashMap<>();

    static {
        for (RequiredSequenceStatusCodes seq : RequiredSequenceStatusCodes.values()) {
            String key = seq.name();
            Set<String> finalEventsDateValidation = getFinalEvents(key);
            Set<String> requiredStatusCodes = getRequiredStatusCodes(seq.getStatusCodeSequence());
            Set<String> sequenceStatusCodes = getSequenceStatusCodes(seq.getStatusCodeSequence());
            Set<String> stockEventsDateValidation = getStockEvents(key);
            Set<String> requiredAttachments = getRequiredAttachments(sequenceStatusCodes);
            Map<String, Set<String>> validAttachments = getValidAttachments(sequenceStatusCodes);
            CONFIGS.put(key, new SequenceConfig(sequenceStatusCodes,
                    requiredStatusCodes, finalEventsDateValidation,
                    stockEventsDateValidation, requiredAttachments, validAttachments));
        }
    }

    private static Set<String> getSequenceStatusCodes(Set<String> statusCodeSequence) {
        return statusCodeSequence.stream()
                .map(code -> code.replace("?",""))
                .collect(Collectors.toSet());
    }

    private static Set<String> getRequiredStatusCodes(Set<String> statusCodeSequence) {
        return statusCodeSequence.stream()
                .filter(code -> !code.contains("?"))
                .map(code -> code.contains(";") ? code.split(";")[0] : code)
                .collect(Collectors.toSet());
    }

    public static Set<String> getRequiredAttachments(Set<String> sequenceStatusCodes) {
        return sequenceStatusCodes.stream()
                .map(statusCode -> Optional.ofNullable(SequenceAttachments.getFromKey(statusCode))
                        .map(SequenceAttachments::getAttachments)
                        .orElse(Set.of()))
                .flatMap(Set::stream)
                .collect(Collectors.toSet())
                .stream()
                .filter(attachment -> !attachment.contains("?"))
                .collect(Collectors.toSet());

    }

    private static Map<String, Set<String>> getValidAttachments(Set<String> sequenceStatusCodes) {
        Map<String, Set<String>> map = new HashMap<>();
        sequenceStatusCodes.forEach(statusCode -> Optional.ofNullable(SequenceAttachments.getFromKey(statusCode))
                        .map(SequenceAttachments::getAttachments)
                        .map(SequenceConfiguration::getAllValidAttachments)
                        .map(attachments -> map.put(statusCode, attachments)));
        return map;
    }

    private static Set<String> getAllValidAttachments(Set<String> attachments) {
        return attachments.stream()
                    .map(attachment -> attachment.replace("?",""))
                    .collect(Collectors.toSet());
    }

    public static SequenceConfig getConfig(String key) {
        SequenceConfig config = CONFIGS.get(key);
        if (config == null) {
            throw new IllegalArgumentException("No configuration found for: " + key);
        }
        return config;
    }


    private static Set<String> getFinalEvents(String key) {
        return Arrays.stream(DateValidationGroup.values())
                .filter(e -> e.name().equalsIgnoreCase(key))
                .findFirst()
                .map(DateValidationGroup::getFinalEventDateValidationGroup)
                .orElse(Set.of());
    }

    private static Set<String> getStockEvents(String key) {
        return Arrays.stream(DateValidationGroup.values())
                .filter(e -> e.name().equalsIgnoreCase(key))
                .findFirst()
                .map(DateValidationGroup::getStockEventsDateValidationGroup)
                .orElse(Set.of());
    }
}