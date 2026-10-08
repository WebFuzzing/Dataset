package it.pagopa.pn.papertracker.model.sequence;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;

/**
 * Enum that defines required attachments for event statusCode.
 * the ? indicates that the attachment is optional.
 *
 */
@Getter
public enum SequenceAttachments {
    RECRN005B(Set.of("Plico")),
    RECRN004B(Set.of("Plico")),
    RECRN003B(Set.of("AR")),
    RECRN002E(Set.of("Plico","?Indagine")),
    RECRN002B(Set.of("Plico")),
    RECRN001B(Set.of("AR")),
    RECRI003B(Set.of("AR")),
    RECRI004B(Set.of("Plico")),
    RECAG001B(Set.of("23L")),
    RECAG002B(Set.of("23L","?CAN")),
    RECAG003B(Set.of("Plico")),
    RECAG003E(Set.of("Plico","?Indagine")),
    RECAG011B(Set.of("23L","ARCAD","CAD")),
    RECAG005B(Set.of("23L","ARCAD","CAD")),
    RECAG006B(Set.of("23L","ARCAD","CAD")),
    RECAG007B(Set.of("23L","ARCAD","CAD","Plico")),
    RECAG008B(Set.of("Plico")),
    RECRS002B(Set.of("Plico")),
    RECRS002E(Set.of("Plico")),
    RECRS004B(Set.of("Plico")),
    RECRS005B(Set.of("Plico")),
    RECRSI004B(Set.of("Plico"));



    private final Set<String> attachments;

    SequenceAttachments(Set<String> attachments) {
        this.attachments = attachments;
    }

    public static SequenceAttachments getFromKey(String statusCode) {
        return Arrays.stream(SequenceAttachments.values())
                .filter(sequenceAttachments -> sequenceAttachments.name().equalsIgnoreCase(statusCode))
                .findFirst()
                .orElse(null);
    }
}
