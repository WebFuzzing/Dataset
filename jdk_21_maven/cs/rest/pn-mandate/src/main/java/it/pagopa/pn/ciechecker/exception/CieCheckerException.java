package it.pagopa.pn.ciechecker.exception;

import it.pagopa.pn.ciechecker.model.ResultCieChecker;

import java.util.Objects;

public class CieCheckerException extends RuntimeException{

    private final ResultCieChecker result;

    public CieCheckerException(){
        super();
        result = null;
    }
    public CieCheckerException(String message){
        super(message);
        result = null;
    }
    public CieCheckerException(Exception cause){
        super(cause);
        result = null;
    }

    public CieCheckerException(ResultCieChecker result) {
        super(Objects.nonNull(result) ? result.getValue() : null);
        this.result = result;
    }

    public CieCheckerException(ResultCieChecker result, Exception cause) {
        super(Objects.nonNull(result) ? result.getValue() : null, cause);
        this.result = result;
    }

    public ResultCieChecker getResult() {
        return result;
    }

}