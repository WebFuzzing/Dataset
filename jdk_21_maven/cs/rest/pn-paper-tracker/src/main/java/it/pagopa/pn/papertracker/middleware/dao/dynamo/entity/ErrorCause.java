package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCause {

    GIACENZA_DATE_ERROR("Date di inizio e fine giacenza non coerenti"),
    OCR_DUPLICATED_EVENT("Evento di risposta duplicato"),
    OCR_DRY_RUN_MODE("Evento di risposta ignorato in quanto attiva la modalità dry-run per l'OCR"),
    OCR_KO( "Errore nella validazione dell'OCR"),
    OCR_UNSUPPORTED_PRODUCT("Prodotto non supportato per la validazione OCR"),
    STOCK_890_REFINEMENT_MISSING("Spedizione 890 non perfezionata"),
    STOCK_890_REFINEMENT_ERROR("Errore nel perfezionamento della spedizione 890"),
    VALUE_AFTER_REFINEMENT("Evento arrivato dopo la conclusione della spedizione o mentre si stava aspettando l’OCR (DONE o AWAITING_OCR)"),
    VALUES_NOT_MATCHING("Errore nella validazione degli allegati della sequenza: mancano degli allegati"),
    INVALID_VALUES("Errore nella validazione"),
    VALUES_NOT_FOUND("Errore nella validazione dei statusCode della sequenza: non sono presenti tutti gli statusCode previsti dalla macchina a stati"),
    SOURCETYPE_FILETYPE_INCOHERENT("Il sourceType e il fileType dell'allegato non sono coerenti");

    private final String description;

}
