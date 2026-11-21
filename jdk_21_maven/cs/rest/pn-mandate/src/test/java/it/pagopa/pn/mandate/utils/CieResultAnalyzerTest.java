package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.mandate.exceptions.PnInvalidCieDataException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CieResultAnalyzerTest {
    private final CieExceptionMapper mapper = mock(CieExceptionMapper.class);
    private final CieResultAnalyzer analyzer = new CieResultAnalyzer(mapper);

    private void verifyCode(PnRuntimeException ex, String cieCheckerServerError) {
        assertEquals(cieCheckerServerError, ex.getProblem().getErrors().get(0).getCode());
    }

    @Test
    @DisplayName("Should return without exception when result is OK")
    void analyzeResultWithOkShouldReturn() {
        assertDoesNotThrow(() -> analyzer.analyzeResult(ResultCieChecker.OK));
    }

    @Test
    @DisplayName("Should throw PnInternalException when result is null")
    void analyzeResultWithNullShouldThrowInternalException() {
        PnInternalException ex = assertThrows(PnInternalException.class, () -> analyzer.analyzeResult(null));
        verifyCode(ex, "CIE_CHECKER_NULL_RESULT");
    }

    @Test
    @DisplayName("Should throw PnInvalidCieDataException for client error result")
    void analyzeResultShouldThrowExceptionFromMapper() {
        when(mapper.mapToException(ResultCieChecker.KO_EXC_INVALID_CMSTYPEDDATA))
                .thenReturn(new PnInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_CMSTYPEDDATA, List.of(CieErrorCategory.CIE_INTEGRITY_ERROR)));
        PnInvalidCieDataException ex = assertThrows(PnInvalidCieDataException.class, () ->
                analyzer.analyzeResult(ResultCieChecker.KO_EXC_INVALID_CMSTYPEDDATA));
        verifyCode(ex, "CIE_INTEGRITY_ERROR");
    }

}