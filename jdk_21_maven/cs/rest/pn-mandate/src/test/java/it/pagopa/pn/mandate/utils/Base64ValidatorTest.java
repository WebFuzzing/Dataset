package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MRTDData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.NISData;
import it.pagopa.pn.mandate.exceptions.PnMandateBadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.*;

class Base64ValidatorTest {
    private final Base64Validator validator = new Base64Validator();

    private void checkExceptionDetails(PnMandateBadRequestException ex, String expectedDetail) {
        assertEquals(expectedDetail, ex.getProblem().getErrors().get(0).getDetail());
    }

    @Test
    @DisplayName("Should validate all fields when all are valid Base64")
    void validateCieValidationDataWithAllValidBase64ShouldPass() {
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("dGVzdA==");
        nisData.setSod("c29k");
        nisData.setPubKey("cHVia2V5");
        MRTDData mrtdData = new MRTDData();
        mrtdData.setSod("bXJ0ZFNvZA==");
        mrtdData.setDg1("ZGcx");
        mrtdData.setDg11("ZGcxMQ==");
        cieValidationData.setNisData(nisData);
        cieValidationData.setMrtdData(mrtdData);
        cieValidationData.setSignedNonce("c2lnbmVkTm9uY2U=");

        assertDoesNotThrow(() -> validator.validateCieValidationData(cieValidationData));
    }

    @ParameterizedTest
    @DisplayName("Should throw exception for invalid, null, or empty Base64 field")
    @MethodSource("invalidBase64Provider")
    void validateCieValidationDataWithInvalidBase64ShouldThrow(String nisValue, String expectedDetail) {
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis(nisValue);
        nisData.setSod("c29k");
        nisData.setPubKey("cHVia2V5");
        MRTDData mrtdData = new MRTDData();
        mrtdData.setSod("bXJ0ZFNvZA==");
        mrtdData.setDg1("ZGcx");
        mrtdData.setDg11("ZGcxMQ==");
        cieValidationData.setNisData(nisData);
        cieValidationData.setMrtdData(mrtdData);
        cieValidationData.setSignedNonce("c2lnbmVkTm9uY2U=");

        PnMandateBadRequestException ex = assertThrows(PnMandateBadRequestException.class, () -> validator.validateCieValidationData(cieValidationData));
        checkExceptionDetails(ex, expectedDetail);
    }

    private static java.util.stream.Stream<Arguments> invalidBase64Provider() {
        return java.util.stream.Stream.of(
                Arguments.of("Zm9vYmFyMTIz%45Nis", "Invalid Base64 encoding in field: nisData.nis"),
                Arguments.of(null, "Missing or empty field: nisData.nis"),
                Arguments.of("", "Missing or empty field: nisData.nis")
        );
    }


}