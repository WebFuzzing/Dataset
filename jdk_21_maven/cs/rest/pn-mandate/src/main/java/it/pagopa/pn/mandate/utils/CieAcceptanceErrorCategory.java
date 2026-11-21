package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.exceptions.PnInvalidCieDataException;
import it.pagopa.pn.mandate.exceptions.PnInvalidMandateStatusException;
import it.pagopa.pn.mandate.exceptions.PnMandateNotFoundException;
import it.pagopa.pn.mandate.exceptions.PnMandatePendingExpiredException;

import java.util.Map;
import java.util.stream.Collectors;

public enum CieAcceptanceErrorCategory {
    FR01, // Anti-cloning
    FR02, // Expired-CIE
    FR03, // CIE not related to the delegator
    FR04, // Data integrity
    MANDATE_EXPIRED, // Mandate expired
    MANDATE_NOT_FOUND, // Mandate not found
    INVALID_MANDATE, // Invalid mandate
    TECH; // Technical error

    /**
     * Mappa statica per associare le eccezioni "semplici" ai codici errore.
     */
    private static final Map<Class<? extends Throwable>, CieAcceptanceErrorCategory> EXCEPTION_MAP = Map.of(
            PnMandatePendingExpiredException.class, MANDATE_EXPIRED,
            PnInvalidMandateStatusException.class, INVALID_MANDATE,
            PnMandateNotFoundException.class, MANDATE_NOT_FOUND
    );

    private static String formatMultipleErrors(PnInvalidCieDataException ex) {
        return ex.getErrorCategories().stream()
                .map(CieAcceptanceErrorCategory::fromCieErrorCategory)
                .map(Enum::name)
                .distinct()
                .collect(Collectors.joining(","));
    }

    private static CieAcceptanceErrorCategory fromCieErrorCategory(CieErrorCategory cieErrorCategory) {
        return switch (cieErrorCategory) {
            case CIE_INVALID_INPUT, CIE_SIGNATURE_ERROR -> FR01;
            case CIE_EXPIRED_ERROR -> FR02;
            case CIE_NOT_RELATED_TO_DELEGATOR_ERROR -> FR03;
            case CIE_INTEGRITY_ERROR -> FR04;
            case CIE_CHECKER_SERVER_ERROR -> TECH;
        };
    }

    public static String fromThrowable(Throwable throwable) {
        // Caso speciale: Gestione dell'eccezione che contiene più categorie
        if (throwable instanceof PnInvalidCieDataException cieEx) {
            return formatMultipleErrors(cieEx);
        }

        // Caso standard: Lookup nella mappa
        return EXCEPTION_MAP.getOrDefault(throwable.getClass(), TECH).name();
    }
}
