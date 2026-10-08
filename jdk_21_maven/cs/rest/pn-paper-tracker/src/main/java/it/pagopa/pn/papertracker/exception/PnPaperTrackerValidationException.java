package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import lombok.Getter;

@Getter
public class PnPaperTrackerValidationException extends RuntimeException {

    private final PaperTrackingsErrors error;

    public PnPaperTrackerValidationException(String message, PaperTrackingsErrors error) {
        super(message);
        this.error = error;
    }
}
