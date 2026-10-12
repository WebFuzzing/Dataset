package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.springframework.http.HttpStatus;

public class PnPaperTrackerBadRequestException extends PnRuntimeException {

    private static final String DEFAULT_ERROR_CODE = "PN_PAPER_TRACKER_BAD_REQUEST";

    public PnPaperTrackerBadRequestException(String errorCode, String message) {
        super(message, DEFAULT_ERROR_CODE, HttpStatus.BAD_REQUEST.value(), errorCode, null, message);
    }
}
