package it.pagopa.pn.mandate.middleware.msclient;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.mandate.exceptions.PnInvalidQrCodeException;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.api.InternalOnlyApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.RequestDecodeQrDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.UserInfoQrCodeDto;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_INTERNAL_SERVER_ERROR;
import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_QR_TOKEN_NOT_FOUND;

@Component
@lombok.CustomLog
@AllArgsConstructor
public class PnDeliveryClient {
    private final InternalOnlyApi internalOnlyApi;

    public Mono<UserInfoQrCodeDto> decodeAarQrCode(String qrToken){
        RequestDecodeQrDtoDto requestDecodeQrDtoDto= new RequestDecodeQrDtoDto();
        requestDecodeQrDtoDto.setAarTokenValue(qrToken);
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_DELIVERY, "Retrieving IUN and recipient information");
        return internalOnlyApi.decodeAarToken(requestDecodeQrDtoDto)
                .onErrorResume(e -> {
                    if(e instanceof WebClientResponseException && ((WebClientResponseException) e).getStatusCode() == HttpStatus.NOT_FOUND){
                        throw new PnInvalidQrCodeException("Qr Token non trovato","Token non è stato trovato",ERROR_CODE_MANDATE_QR_TOKEN_NOT_FOUND);
                    }
                    throw new PnInternalException("Internal Server Error",ERROR_CODE_MANDATE_INTERNAL_SERVER_ERROR,e);
                });
    }
}
