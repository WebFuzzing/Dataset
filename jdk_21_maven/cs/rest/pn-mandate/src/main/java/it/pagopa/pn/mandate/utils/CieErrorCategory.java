package it.pagopa.pn.mandate.utils;

import lombok.Getter;

@Getter
public enum CieErrorCategory {
    CIE_INVALID_INPUT("Invalid input data provided"), // Missing or malformed input
    CIE_INTEGRITY_ERROR("Data integrity check failed"), // Data integrity issues
    CIE_SIGNATURE_ERROR("Signature verification failed"), // Signature verification issues
    CIE_NOT_RELATED_TO_DELEGATOR_ERROR("CIE is valid, but not related to the delegator of given mandate"),
    CIE_EXPIRED_ERROR("CIE has expired"), // CIE expired
    CIE_CHECKER_SERVER_ERROR("A technical error occurred during CIE validation"); // Generic error

    private final String code;
    private final String message;

    CieErrorCategory(String message) {
        this.code = this.name();
        this.message = message;
    }
}
