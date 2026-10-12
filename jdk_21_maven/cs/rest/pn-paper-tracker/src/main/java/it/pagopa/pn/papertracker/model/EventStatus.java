package it.pagopa.pn.papertracker.model;

import lombok.Getter;

@Getter
public enum EventStatus {

    KO("KO"),
    PROGRESS("PROGRESS"),
    OK("OK");

    private final String value;

    EventStatus(String value) {
        this.value = value;
    }
}
