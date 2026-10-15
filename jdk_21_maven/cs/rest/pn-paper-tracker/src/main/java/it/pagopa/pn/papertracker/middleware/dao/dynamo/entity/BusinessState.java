package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

public enum BusinessState {
    AWAITING_FINAL_STATUS_CODE,
    AWAITING_REFINEMENT_OCR,
    AWAITING_REWORK_EVENTS,
    AWAITING_OCR,
    DONE,
    KO

}

