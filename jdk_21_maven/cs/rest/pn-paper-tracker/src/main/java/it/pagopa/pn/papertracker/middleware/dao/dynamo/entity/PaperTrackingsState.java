package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

public enum PaperTrackingsState {
    AWAITING_FINAL_STATUS_CODE, //mantenuto per retrocompatibilità
    AWAITING_REFINEMENT,
    AWAITING_REWORK_EVENTS,
    AWAITING_OCR,
    DONE,
    KO
}
