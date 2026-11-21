package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_NOT_FOUND;

public class PnForbiddenException extends PnRuntimeException {

    public PnForbiddenException() {
        super("Accesso negato!", "L'utente non è autorizzato ad accedere alla risorsa richiesta.",
                HttpStatus.NOT_FOUND.value(), ERROR_CODE_MANDATE_NOT_FOUND, null, null);
    }

}
