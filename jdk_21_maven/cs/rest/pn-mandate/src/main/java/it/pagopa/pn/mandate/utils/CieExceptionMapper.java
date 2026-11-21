package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.PnRuntimeException;
import it.pagopa.pn.mandate.exceptions.PnCieInvalidVerificationCodeException;
import it.pagopa.pn.mandate.exceptions.PnInvalidCieDataException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static it.pagopa.pn.mandate.utils.CieErrorCategory.CIE_CHECKER_SERVER_ERROR;

@Component
public class CieExceptionMapper {
    private final Map<ResultCieChecker, Supplier<? extends PnRuntimeException>> resultToExceptionMap = new HashMap<>();

    public CieExceptionMapper() {
        buildExceptionMap();
    }

    private void buildExceptionMap() {
        /*
            Gli inserimenti commentati con "Dovrebbe essere categorizzata anche come TECH" sono lasciati per eventuali sviluppi futuri.
            Al momento per ragioni di retrocompatibilità si decide di lasciare solo le categorie gestite.
        */
        putCustomException(ResultCieChecker.KO_EXC_NO_MATCH_NONCE_SIGNATURE, () -> new PnCieInvalidVerificationCodeException(ResultCieChecker.KO_EXC_NO_MATCH_NONCE_SIGNATURE));
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_CIESOD, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_CIENIS, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_PUBLICKEY, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_SIGNEDNONCE, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_NONCE, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_MRTDSOD, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_MRTDDG1, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_MRTDDG11, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_CODFISCDELEGANTE, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOTFOUND_CERTIFICATES, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOTFOUND_CMSSIGNEDDATA, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_CERTIFICATE_NOT_SIGNED, CieErrorCategory.CIE_SIGNATURE_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_SIGNERINFORMATION, CieErrorCategory.CIE_SIGNATURE_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_PARSING_CERTIFICATION, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_GENERATE_CERTIFICATE, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_GENERATE_CMSSIGNEDDATA, CieErrorCategory.CIE_INTEGRITY_ERROR);  // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_VALIDATE_CERTIFICATE, List.of(CieErrorCategory.CIE_SIGNATURE_ERROR, CieErrorCategory.CIE_INTEGRITY_ERROR));
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_SUPPORTED_CERTIFICATEPATHVALIDATOR, List.of(CieErrorCategory.CIE_INVALID_INPUT, CieErrorCategory.CIE_INTEGRITY_ERROR));
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PARAMETER_CERTPATHVALIDATOR, List.of(CieErrorCategory.CIE_INVALID_INPUT, CieErrorCategory.CIE_INTEGRITY_ERROR));
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_HASH_ALGORITHM_SOD, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_HASH_SIGNED_DATA, CieErrorCategory.CIE_INTEGRITY_ERROR);  // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_UNSUPPORTED_ALGORITHM_SOD, CieErrorCategory.CIE_INVALID_INPUT);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOTFOUND_EXPECTED_HASHES_SOD, CieErrorCategory.CIE_INTEGRITY_ERROR);  // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOTFOUND_DIGEST_SOD, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOTFOUND_MRTD_SOD, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_GENERATE_PUBLICKEY, CieErrorCategory.CIE_SIGNATURE_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_EXTRACTION_PUBLICKEY, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_CRYPTOGRAPHIC_OPERATION, CieErrorCategory.CIE_SIGNATURE_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_SIGNATURES_SIGNED_DATA, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_SIGNERINFORMATIONSTORE, List.of(CieErrorCategory.CIE_INVALID_INPUT, CieErrorCategory.CIE_INTEGRITY_ERROR));
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_MESSAGEDIGESTSPI_SUPPORTED, CieErrorCategory.CIE_INVALID_INPUT);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_HASH_CONTENT_MATCH, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_SIGNED_ATTRIBUTE, CieErrorCategory.CIE_INTEGRITY_ERROR); // Dovrebbe essere categorizzata anche come TECH
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_MATCH_NIS_HASHES_DATAGROUP, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_CMSTYPEDDATA, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_CMSTYPEDDATA, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NO_NIS_HASHES_DATAGROUP, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_ERROR_CREATE_VERIFIER, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOVALID_DIGITAL_SIGNATURE, CieErrorCategory.CIE_INVALID_INPUT);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOT_SAME_DIGEST1, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOT_SAME_DIGEST11, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_DIGEST_NOT_VERIFIED, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_ERROR_SOD_DECODE, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_SIGNATURE, CieErrorCategory.CIE_SIGNATURE_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_ALGORITHM, CieErrorCategory.CIE_SIGNATURE_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_PUBLICKEY, CieErrorCategory.CIE_SIGNATURE_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_DECODER_ERROR, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOFOUND_CODFISCALE_DG11, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_NOFOUND_EXPIRE_DATE_DG1, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_INVALID_EXPIRATIONDATE, CieErrorCategory.CIE_INTEGRITY_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_EXPIRATIONDATE, CieErrorCategory.CIE_EXPIRED_ERROR);
        putInvalidCieDataException(ResultCieChecker.KO_EXC_CODFISCALE_NOT_VERIFIED, CieErrorCategory.CIE_NOT_RELATED_TO_DELEGATOR_ERROR);
    }

    private void putInvalidCieDataException(ResultCieChecker resultCieChecker, CieErrorCategory category) {
        this.putCustomException(resultCieChecker, () -> new PnInvalidCieDataException(resultCieChecker, Collections.singletonList(category)));
    }

    private void putInvalidCieDataException(ResultCieChecker resultCieChecker, List<CieErrorCategory> categories) {
        this.putCustomException(resultCieChecker, () -> new PnInvalidCieDataException(resultCieChecker, categories));
    }

    private void putCustomException(ResultCieChecker resultCieChecker, Supplier<? extends PnInvalidCieDataException> exceptionSupplier) {
        if(resultToExceptionMap.containsKey(resultCieChecker)) {
            throw new IllegalStateException("Result " + resultCieChecker + " is already mapped to an exception");
        }

        this.resultToExceptionMap.put(resultCieChecker, exceptionSupplier);
    }

    public PnRuntimeException mapToException(ResultCieChecker result) {
        Supplier<? extends PnRuntimeException> exceptionSupplier = resultToExceptionMap.get(result);

        if (exceptionSupplier != null) {
            return exceptionSupplier.get();
        }

        String errorMessage = String.format("CIE Checker server error for code: %s", result.name());
        return new PnInternalException(errorMessage, CIE_CHECKER_SERVER_ERROR.name());
    }
}
