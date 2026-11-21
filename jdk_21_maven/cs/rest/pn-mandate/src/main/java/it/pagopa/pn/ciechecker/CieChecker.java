package it.pagopa.pn.ciechecker;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;

public interface CieChecker {

    void init() throws CieCheckerException;

    ResultCieChecker validateMandate(CieValidationData data);
}
