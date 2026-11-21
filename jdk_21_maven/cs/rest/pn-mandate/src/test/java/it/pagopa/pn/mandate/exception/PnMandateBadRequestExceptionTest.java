package it.pagopa.pn.mandate.exception;

import it.pagopa.pn.mandate.exceptions.PnMandateBadRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PnMandateBadRequestExceptionTest {

    @Test
    void testConstructor() {
        PnMandateBadRequestException exception = new PnMandateBadRequestException();

        Assertions.assertEquals(exception.getStatus(), HttpStatus.BAD_REQUEST.value());
    }
}