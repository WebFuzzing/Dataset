package it.pagopa.pn.mandate.utils;

import it.pagopa.pn.mandate.exceptions.PnInvalidQrCodeException;
import it.pagopa.pn.mandate.springbootcfg.QrUrlCodecConsumerActivation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_NOT_VALID_AARQRCODE_ERROR;

@Component
@AllArgsConstructor
public class AarQrUtils {
    private final QrUrlCodecConsumerActivation qrUrlCodecConsumerActivation;

    public String extractQrToken(String url){
        try{
            return qrUrlCodecConsumerActivation.decode(url);
        }
        catch (IllegalArgumentException e){
            throw new PnInvalidQrCodeException("Qr code non valido","aarQrCode fornito in input non è valido", ERROR_CODE_MANDATE_NOT_VALID_AARQRCODE_ERROR);
        }
    }
}
