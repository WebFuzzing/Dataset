package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.mandate.utils.CieErrorCategory;

import java.util.List;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_INVALID_VERIFICATION_CODE;

public class PnCieInvalidVerificationCodeException extends PnInvalidCieDataException {

    public PnCieInvalidVerificationCodeException(ResultCieChecker resultCieChecker) {
        super(ERROR_CODE_INVALID_VERIFICATION_CODE, "Nonce signature verification failed", resultCieChecker, List.of(CieErrorCategory.CIE_SIGNATURE_ERROR));
    }
}
