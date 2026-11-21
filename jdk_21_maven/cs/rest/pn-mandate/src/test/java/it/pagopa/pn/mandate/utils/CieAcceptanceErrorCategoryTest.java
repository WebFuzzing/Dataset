package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.exceptions.PnInvalidCieDataException;
import it.pagopa.pn.mandate.exceptions.PnInvalidMandateStatusException;
import it.pagopa.pn.mandate.exceptions.PnMandateNotFoundException;
import it.pagopa.pn.mandate.exceptions.PnMandatePendingExpiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.ciechecker.model.ResultCieChecker.KO;
import static org.junit.jupiter.api.Assertions.*;

class CieAcceptanceErrorCategoryTest {
    @Test
    @DisplayName("Should return MANDATE_EXPIRED for PnMandatePendingExpiredException")
    void fromThrowableWithMandateExpiredExceptionReturnsMandateExpired() {
        String result = CieAcceptanceErrorCategory.fromThrowable(new PnMandatePendingExpiredException());
        assertEquals(CieAcceptanceErrorCategory.MANDATE_EXPIRED.name(), result);
    }

    @Test
    @DisplayName("Should return INVALID_MANDATE for PnInvalidMandateStatusException")
    void fromThrowableWithInvalidMandateStatusExceptionReturnsInvalidMandate() {
        String result = CieAcceptanceErrorCategory.fromThrowable(new PnInvalidMandateStatusException("invalid", "expected", 400, "INVALID", null));
        assertEquals(CieAcceptanceErrorCategory.INVALID_MANDATE.name(), result);
    }

    @Test
    @DisplayName("Should return MANDATE_NOT_FOUND for PnMandateNotFoundException")
    void fromThrowableWithMandateNotFoundExceptionReturnsMandateNotFound() {
        String result = CieAcceptanceErrorCategory.fromThrowable(new PnMandateNotFoundException());
        assertEquals(CieAcceptanceErrorCategory.MANDATE_NOT_FOUND.name(), result);
    }

    @Test
    @DisplayName("Should return TECH for unknown exception")
    void fromThrowableWithUnknownExceptionReturnsTech() {
        String result = CieAcceptanceErrorCategory.fromThrowable(new RuntimeException("generic"));
        assertEquals(CieAcceptanceErrorCategory.TECH.name(), result);
    }

    @Test
    @DisplayName("Should return comma separated error categories for PnInvalidCieDataException with multiple errors")
    void fromThrowableWithInvalidCieDataExceptionReturnsMultipleCategories() {
        PnInvalidCieDataException ex = new PnInvalidCieDataException(
                KO,
                List.of(CieErrorCategory.CIE_EXPIRED_ERROR, CieErrorCategory.CIE_INTEGRITY_ERROR)
        );
        String result = CieAcceptanceErrorCategory.fromThrowable(ex);
        assertEquals("FR02,FR04", result);
    }

    @Test
    @DisplayName("Should return TECH for PnInvalidCieDataException with only tech error")
    void fromThrowableWithInvalidCieDataExceptionWithTechErrorReturnsTech() {
        PnInvalidCieDataException ex = new PnInvalidCieDataException(
                KO,
                List.of(CieErrorCategory.CIE_CHECKER_SERVER_ERROR)
        );
        String result = CieAcceptanceErrorCategory.fromThrowable(ex);
        assertEquals("TECH", result);
    }

    @Test
    @DisplayName("Should return FR03 for PnInvalidCieDataException with not related to delegator error")
    void fromThrowableWithInvalidCieDataExceptionWithNotRelatedToDelegatorErrorReturnsFR03() {
        PnInvalidCieDataException ex = new PnInvalidCieDataException(
                KO,
                List.of(CieErrorCategory.CIE_NOT_RELATED_TO_DELEGATOR_ERROR)
        );
        String result = CieAcceptanceErrorCategory.fromThrowable(ex);
        assertEquals("FR03", result);
    }
}