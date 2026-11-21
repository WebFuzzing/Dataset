package it.pagopa.pn.mandate.utils;

import lombok.Getter;

@Getter
public enum RevocationCause {
    USER("- requested by user"),
    SYSTEM("- requested by system");

    private final String logSuffix;

    RevocationCause(String logSuffix) {
        this.logSuffix = logSuffix;
    }
}
