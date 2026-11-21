package it.pagopa.pn.mandate.exception;

import it.pagopa.pn.mandate.exceptions.PnMandateAlreadyExistsException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class PnMandateAlreadyExiststExceptionTest {

    @Test
    void pnInvalidInputExceptionConstructorTest() {
        PnMandateAlreadyExistsException exception = new PnMandateAlreadyExistsException();

        Assertions.assertEquals(exception.getStatus(), HttpStatus.CONFLICT.value());
    }

}