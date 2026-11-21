package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

public class PnInvalidQrCodeException extends PnRuntimeException {
    public PnInvalidQrCodeException(String message, String description, String errorcode) {
        super(message,description, HttpStatus.BAD_REQUEST.value(), errorcode, null, null);
    }
}
