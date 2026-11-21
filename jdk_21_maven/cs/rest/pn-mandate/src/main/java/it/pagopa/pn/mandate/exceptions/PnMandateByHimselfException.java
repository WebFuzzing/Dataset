package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_DELEGATE_HIMSELF;

public class PnMandateByHimselfException extends PnRuntimeException {

    public PnMandateByHimselfException() {
        super("Delega già presente", "Non è possibile delegare se stessi", HttpStatus.CONFLICT.value(), ERROR_CODE_MANDATE_DELEGATE_HIMSELF, null, null);
    }

}
