package it.pagopa.pn.mandate.exceptions;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;

import java.util.List;

public class PnUnsupportedFilterException extends PnValidationException {

    public PnUnsupportedFilterException(String errorCode, String field) {
        super("Filtro non valido", List.of(ProblemError.builder()
                        .code(errorCode)
                        .element(field)
                .build()), null );
    }

}
