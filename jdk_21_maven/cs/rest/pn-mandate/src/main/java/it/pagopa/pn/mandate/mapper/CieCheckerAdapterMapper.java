package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.ciechecker.model.CieIas;
import it.pagopa.pn.ciechecker.model.CieMrtd;
import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MRTDData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.NISData;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class CieCheckerAdapterMapper implements CieCheckerAdapterMapperInterface{
    private static final String INVALID_BASE_64_MESSAGE = "Invalid base64 encoding for field: %s";

    @Override
    public CieValidationData mapToLibDto(CIEValidationData cieValidationDataInput, String nonce, String delegatorTaxId) {
        CieValidationData cieValidationDataLib = new CieValidationData();
        cieValidationDataLib.setCieIas(mapIas(cieValidationDataInput.getNisData()));
        cieValidationDataLib.setCieMrtd(mapMrtd(cieValidationDataInput.getMrtdData()));
        cieValidationDataLib.setSignedNonce(decodeBase64(cieValidationDataInput.getSignedNonce(), "signedNonce"));
        cieValidationDataLib.setNonce(nonce);
        cieValidationDataLib.setCodFiscDelegante(delegatorTaxId);
        return cieValidationDataLib;
    }

    private CieIas mapIas(NISData nisData) {
        CieIas cieIas = new CieIas();
        cieIas.setNis(decodeBase64(nisData.getNis(), "nisData.nis"));
        cieIas.setSod(decodeBase64(nisData.getSod(), "nisData.sod"));
        cieIas.setPublicKey(decodeBase64(nisData.getPubKey(), "nisData.pubKey"));
        return cieIas;
    }

    private CieMrtd mapMrtd(MRTDData mrtdData) {
        CieMrtd cieMrtd = new CieMrtd();
        cieMrtd.setSod(decodeBase64(mrtdData.getSod(), "mrtdData.sod"));
        cieMrtd.setDg1(decodeBase64(mrtdData.getDg1(), "mrtdData.dg1"));
        cieMrtd.setDg11(decodeBase64(mrtdData.getDg11(), "mrtdData.dg11"));
        return cieMrtd;
    }

    private byte[] decodeBase64(String base64String, String fieldName) {
        try {
            return Base64.getUrlDecoder().decode(base64String);
        } catch(IllegalArgumentException ex) {
            String errorMessage = String.format(INVALID_BASE_64_MESSAGE, fieldName);
            throw new PnInternalException("Invalid base64 encoding", errorMessage);
        }
    }
}
