package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import org.apache.commons.codec.DecoderException;

public interface CieCheckerAdapterMapperInterface{
    CieValidationData mapToLibDto(CIEValidationData cieValidationDataInput, String nonce, String delegatorTaxId) throws DecoderException;
}
