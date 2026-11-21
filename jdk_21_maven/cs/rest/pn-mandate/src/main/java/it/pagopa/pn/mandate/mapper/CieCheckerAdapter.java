package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.ciechecker.exception.CieCheckerException;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;

public interface CieCheckerAdapter {
    void validateCie(CIEValidationData data, String nonce, String delegatorTaxId) throws CieCheckerException;
}
