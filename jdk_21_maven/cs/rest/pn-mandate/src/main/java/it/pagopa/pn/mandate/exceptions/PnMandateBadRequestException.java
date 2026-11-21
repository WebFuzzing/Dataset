package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_BAD_REQUEST;

public class PnMandateBadRequestException extends PnRuntimeException {
    public PnMandateBadRequestException( ) {
        super("Richiesta non valida", "La richiesta effettuata non è valida", HttpStatus.BAD_REQUEST.value(),ERROR_CODE_MANDATE_BAD_REQUEST,null, null);
    }

    public PnMandateBadRequestException(@NotNull String message, @NotNull String description, @NotNull String errorcode) {
        super(message, description, HttpStatus.BAD_REQUEST.value(), errorcode, null, null);
    }

    public PnMandateBadRequestException(@NotNull String message, @NotNull String description, @NotNull String errorcode, String detail) {
        super(message, description, HttpStatus.BAD_REQUEST.value(), errorcode, null, detail);
    }
}
