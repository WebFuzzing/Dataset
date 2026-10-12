package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.http.HttpStatus;

public class PnPaperTrackerNotFoundException extends PnInternalException {

    public PnPaperTrackerNotFoundException(String errorCode, String message) {
        super(message, HttpStatus.NOT_FOUND.value(), errorCode);
    }
}
