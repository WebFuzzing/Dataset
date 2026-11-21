package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.jetbrains.annotations.NotNull;

public class PnInvalidMandateStatusException extends PnRuntimeException {

    public PnInvalidMandateStatusException(@NotNull String message, @NotNull String description, int status, @NotNull String errorcode, String detail) {
        super(message, description, status, errorcode, null, detail);
    }
}
