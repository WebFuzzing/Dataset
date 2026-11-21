package it.pagopa.pn.mandate.exception;

import it.pagopa.pn.mandate.exceptions.PnInvalidInputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

class PnInvalidInputExceptionTest {

    private final String code = "405";


    @Test
    void pnInvalidInputExceptionConstructorTest() {
        String field = "test-";
        PnInvalidInputException pnInvalidInputException = new PnInvalidInputException(code, field);

        Assertions.assertEquals(pnInvalidInputException.getProblem().getErrors().get(0).getCode(), code);
        Assertions.assertEquals(pnInvalidInputException.getProblem().getErrors().get(0).getElement(), field);
    }

    @Test
    void pnInvalidInputExceptionConstructor2Test() {
        PnInvalidInputException pnInvalidInputException = new PnInvalidInputException(code, new ArrayList<>());

        Assertions.assertEquals("PN_GENERIC_ERROR", pnInvalidInputException.getProblem().getErrors().get(0).getCode());
    }

}