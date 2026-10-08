package it.pagopa.pn.papertracker.model;

import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum DocumentTypeEnum {
    PLICO("Plico"),
    AR("AR"),
    INDAGINE("Indagine"),
    _23L("23L"),
    ARCAD("ARCAD"),
    CAD("CAD"),
    CAN("CAN");

    private final String value;

    DocumentTypeEnum(String value) {
        this.value = value;
    }

    public static DocumentTypeEnum fromValue(String documentType) {
        return Arrays.stream(DocumentTypeEnum.values())
                .filter(documentTypeEnum -> documentTypeEnum.getValue().equalsIgnoreCase(documentType))
                .findFirst()
                .orElseThrow(() -> new PaperTrackerException("Invalid document type: " + documentType));
    }
}
