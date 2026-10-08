package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Getter;

@Getter
public enum ErrorCategory {

    TRACKING_ID_NOT_FOUND("TrackingId non trovato"),
    NOT_RETRYABLE_EVENT_ERROR("Evento not retryable ricevuto"),
    RENDICONTAZIONE_SCARTATA("Rendicontazione scartata"),
    DATE_ERROR("Errore nella validazione delle date della sequenza."),
    LAST_EVENT_EXTRACTION_ERROR("Errore nell'estrazione della sequenza dall'ultimo evento."),
    REGISTERED_LETTER_CODE_ERROR("Errore nella validazione del registered letter code"),
    DELIVERY_FAILURE_CAUSE_ERROR("Errore nella validazione del delivery Failure Cause"),
    ATTACHMENTS_ERROR("Errore nella validazione degli allegati della sequenza"),
    MAX_RETRY_REACHED_ERROR("Numero massimo di retry raggiunto"),
    OCR_VALIDATION("Errore nella validazione OCR"),
    DEMAT_EMPTY_EVENT("Errore evento per demat non trovato"),
    DEMAT_ATTACHMENT_NUMBER_ERROR("Errore nel numero di attachment trovati per demat"),
    DUPLICATED_EVENT("Errore nella validazione della presenza di eventi duplicati"),
    INVALID_STATE_FOR_STOCK_890("Stato non valido per giacenza 890"),
    INCONSISTENT_STATE("Tracking completato o in attesa validazione OCR"),
    /**
     * @deprecated Non utilizzare. Campo mantenuto solo per retrocompatibilità.
     */
    @Deprecated
    STATUS_CODE_ERROR("Errore nella validazione della presenza degli elementi della sequenza."),
    REGISTERED_LETTER_CODE_NOT_FOUND("Registered letter code non trovato");

    private final String value;

    ErrorCategory(String value) {
        this.value = value;
    }

}
