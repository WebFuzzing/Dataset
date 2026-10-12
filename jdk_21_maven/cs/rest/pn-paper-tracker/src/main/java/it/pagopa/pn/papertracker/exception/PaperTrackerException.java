package it.pagopa.pn.papertracker.exception;

public class PaperTrackerException extends RuntimeException {

    public PaperTrackerException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public PaperTrackerException(String message) {
        super(message);
    }
}