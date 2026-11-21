package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_ALREADY_EXISTS;

public class PnMandateAlreadyExistsException extends PnRuntimeException {

    public PnMandateAlreadyExistsException() {
        super("Delega già presente", "Non è possibile creare due deleghe per lo stesso delegato", HttpStatus.CONFLICT.value(), ERROR_CODE_MANDATE_ALREADY_EXISTS, null, null);
    }

}
