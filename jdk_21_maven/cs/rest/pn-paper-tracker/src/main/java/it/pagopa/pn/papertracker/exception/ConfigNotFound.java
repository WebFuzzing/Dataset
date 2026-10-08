package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.http.HttpStatus;

public class ConfigNotFound extends PnInternalException {

    public ConfigNotFound(String errorCode, String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR.value(), errorCode);
    }
}
