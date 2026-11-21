package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_INVALID_VERIFICATION_CODE;

public class PnInvalidVerificationCodeException extends PnRuntimeException {

    public PnInvalidVerificationCodeException() {
        super("Codice verifica non valido", "Il codice passato non è corretto", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_INVALID_VERIFICATION_CODE, null, null);
    }

    public PnInvalidVerificationCodeException(String detail) {
        super("Codice verifica non valido", "Il codice passato non è corretto", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_INVALID_VERIFICATION_CODE, null, detail);
    }

}
