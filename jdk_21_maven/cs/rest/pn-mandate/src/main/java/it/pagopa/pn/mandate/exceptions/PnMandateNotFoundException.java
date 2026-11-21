package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_NOT_FOUND;

public class PnMandateNotFoundException extends PnRuntimeException {

    public PnMandateNotFoundException() {
        super("Delega non presente", "Non è stata trovata nessuna delega valida", HttpStatus.NOT_FOUND.value(), ERROR_CODE_MANDATE_NOT_FOUND, null, null);
    }
    public PnMandateNotFoundException(@NotNull String message, @NotNull String description, @NotNull String errorcode) {
        super(message, description, HttpStatus.NOT_FOUND.value(), errorcode, null, null);
    }

}
