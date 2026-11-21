package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.ciechecker.model.CieValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MRTDData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.NISData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CieCheckerAdapterMapperTest {

    private final CieCheckerAdapterMapper mapper = new CieCheckerAdapterMapper();

    @Test
    @DisplayName("Should throw PnInternalException when NISData.nis is invalid base64")
    void mapToLibDtoWithInvalidNisBase64ShouldThrowException() {
        NISData nisData = new NISData();
        nisData.setNis("Zm9vYmFyMTIz%45Nis");
        nisData.setSod("c29k");
        nisData.setPubKey("cHVia2V5");
        MRTDData mrtdData = new MRTDData();
        mrtdData.setSod("bXJ0ZFNvZA==");
        mrtdData.setDg1("ZGcx");
        mrtdData.setDg11("ZGcxMQ==");
        CIEValidationData input = new CIEValidationData();
        input.setNisData(nisData);
        input.setMrtdData(mrtdData);
        input.setSignedNonce("c2lnbmVkTm9uY2U=");

        assertThrows(it.pagopa.pn.commons.exceptions.PnInternalException.class, () ->
                mapper.mapToLibDto(input, "nonceValue", "ABCDEF12G34H567I"));
    }

    @Test
    @DisplayName("Should map empty strings as empty byte arrays")
    void mapToLibDtoWithEmptyStringsShouldMapToEmptyByteArrays() {
        NISData nisData = new NISData();
        nisData.setNis("");
        nisData.setSod("");
        nisData.setPubKey("");
        MRTDData mrtdData = new MRTDData();
        mrtdData.setSod("");
        mrtdData.setDg1("");
        mrtdData.setDg11("");
        CIEValidationData input = new CIEValidationData();
        input.setNisData(nisData);
        input.setMrtdData(mrtdData);
        input.setSignedNonce("");

        CieValidationData result = mapper.mapToLibDto(input, "nonceValue", "ABCDEF12G34H567I");

        assertNotNull(result);
        assertArrayEquals(new byte[0], result.getCieIas().getNis());
        assertArrayEquals(new byte[0], result.getCieIas().getSod());
        assertArrayEquals(new byte[0], result.getCieIas().getPublicKey());
        assertArrayEquals(new byte[0], result.getCieMrtd().getSod());
        assertArrayEquals(new byte[0], result.getCieMrtd().getDg1());
        assertArrayEquals(new byte[0], result.getCieMrtd().getDg11());
        assertArrayEquals(new byte[0], result.getSignedNonce());
    }
}
