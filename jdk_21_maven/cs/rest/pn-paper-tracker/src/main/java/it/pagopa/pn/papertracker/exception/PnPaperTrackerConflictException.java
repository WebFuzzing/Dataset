package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.http.HttpStatus;

public class PnPaperTrackerConflictException extends PnInternalException {

    public PnPaperTrackerConflictException(String errorCode, String message) {
        super(message, HttpStatus.CONFLICT.value(), errorCode);
    }
}
