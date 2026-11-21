package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.commons.exceptions.PnExceptionsCodes.ERROR_CODE_PN_GENERIC_INVALIDPARAMETER;


public class PnInvalidVisibilityIdException extends PnRuntimeException {

    public PnInvalidVisibilityIdException() {
        super("VisibilityId", "id non radice presente in visibilityId", HttpStatus.UNPROCESSABLE_ENTITY.value(), ERROR_CODE_PN_GENERIC_INVALIDPARAMETER, null, null);
    }

}
