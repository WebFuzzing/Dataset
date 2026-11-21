package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_INVALID_GROUP;

public class PnInvalidGroupCodeException extends PnRuntimeException {

    public PnInvalidGroupCodeException() {
        super("Id gruppo", "Il gruppo non è presente o non è attivo", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_INVALID_GROUP, null, null);
    }

}
