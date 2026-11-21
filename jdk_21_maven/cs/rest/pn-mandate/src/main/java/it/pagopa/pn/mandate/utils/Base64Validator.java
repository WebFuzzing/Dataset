package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.exceptions.PnMandateBadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_BAD_REQUEST;

@Component
@Slf4j
public class Base64Validator {

    public void validateCieValidationData(CIEValidationData cieValidationData) {
        log.debug("Validating CIEValidationData fields for Base64 encoding");
        List<Pair<String, String>> fieldsToValidate = List.of(
            Pair.of(cieValidationData.getMrtdData().getSod(), "mrtdData.sod"),
            Pair.of(cieValidationData.getMrtdData().getDg1(), "mrtdData.dg1"),
            Pair.of(cieValidationData.getMrtdData().getDg11(), "mrtdData.dg11"),
            Pair.of(cieValidationData.getNisData().getNis(), "nisData.nis"),
            Pair.of(cieValidationData.getNisData().getSod(), "nisData.sod"),
            Pair.of(cieValidationData.getNisData().getPubKey(), "nisData.pub_key"),
            Pair.of(cieValidationData.getSignedNonce(), "signedNonce")
        );

        for (Pair<String, String> field : fieldsToValidate) {
            validateBase64(field.getLeft(), field.getRight());
        }
        log.debug("All CIEValidationData fields are valid Base64");
    }

    private void validateBase64(String base64String, String fieldName) {
        log.debug("Validating field: {}", fieldName);
        if (base64String == null || base64String.isEmpty()) {
            String missingFieldLog = String.format("Missing or empty field: %s", fieldName);
            throw new PnMandateBadRequestException("Mandate Bad Request", "CIE Data missing values", ERROR_CODE_MANDATE_BAD_REQUEST, missingFieldLog);
        }

        boolean isValid = isValidDefaultBase64Standard(base64String) || isValidBase64UrlEncoded(base64String);

        if(!isValid) {
            String invalidFieldLog = String.format("Invalid Base64 encoding in field: %s", fieldName);
            throw new PnMandateBadRequestException("Mandate Bad Request", "CIE Data with invalid encoding", ERROR_CODE_MANDATE_BAD_REQUEST, invalidFieldLog);
        }
    }

    private boolean isValidDefaultBase64Standard(String base64String) {
        try {
            Base64.getDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }

    private boolean isValidBase64UrlEncoded(String base64String) {
        try {
            Base64.getUrlDecoder().decode(base64String);
        } catch (IllegalArgumentException e) {
            return false;
        }

        return true;
    }
}
