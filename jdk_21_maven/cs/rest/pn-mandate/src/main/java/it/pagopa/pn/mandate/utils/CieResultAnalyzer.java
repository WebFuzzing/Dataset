package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.mandate.exceptions.PnInvalidCieDataException;
import it.pagopa.pn.mandate.exceptions.PnInvalidVerificationCodeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CieResultAnalyzer {
    private final CieExceptionMapper exceptionMapper;

    /**
     * Analyze the result of the CIE Checker and throw appropriate exceptions based on the result.
     * If the result is OK, the method simply returns.
     * @param checkerResult the result from the CIE Checker to analyze
     * @throws PnInvalidVerificationCodeException If error is caused by invalid verification code (422).
     * @throws PnInvalidCieDataException If error is caused by client data (422).
     * @throws PnInternalException If error is caused by internal system problems (500).
     */
    public void analyzeResult(ResultCieChecker checkerResult) {
        log.info("Analyzing CIE Checker result: {}", checkerResult);
        if (checkerResult == null) {
            log.error("CIE Checker returned a null result");
            throw new PnInternalException("CIE Checker returned a null result", "CIE_CHECKER_NULL_RESULT");
        }
        if (checkerResult == ResultCieChecker.OK) {
            log.debug("CIE Checker validation successful (OK).");
            return;
        }

        RuntimeException exception = exceptionMapper.mapToException(checkerResult);
        log.warn("CIE Checker error identified: {} -> Exception: {}",
                checkerResult.name(), exception.getClass().getSimpleName());
        throw exception;
    }
}