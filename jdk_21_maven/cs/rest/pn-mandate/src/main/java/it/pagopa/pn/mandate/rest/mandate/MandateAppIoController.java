package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationRequest;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationResponse;
import it.pagopa.pn.mandate.appio.reverse.generated.openapi.server.v1.api.AppIoPnMandateCreateApi;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@lombok.CustomLog
@AllArgsConstructor
public class MandateAppIoController implements AppIoPnMandateCreateApi {
    private final MandateService mandateService;

    @Override
    public Mono<ResponseEntity<Void>> acceptIOMandate(String xPagopaPnCxId, CxTypeAuthFleet xPagopaPnCxType, String mandateId, Mono<CIEValidationData> ciEValidationData,  final ServerWebExchange exchange) {
        return mandateService.acceptMandateAppIo(xPagopaPnCxId, xPagopaPnCxType, mandateId, ciEValidationData)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @Override
    public Mono<ResponseEntity<MandateCreationResponse>> createIOMandate(String xPagopaPnCxId, CxTypeAuthFleet xPagopaPnCxType, String xPagopaLollipopUserName, String xPagopaLollipopUserFamilyName, Mono<MandateCreationRequest> mandateCreationRequest,  final ServerWebExchange exchange) {
        return mandateService.createMandateAppIo(xPagopaPnCxId, xPagopaLollipopUserName, xPagopaLollipopUserFamilyName, xPagopaPnCxType, mandateCreationRequest)
                .map(response ->ResponseEntity.status(HttpStatus.CREATED).body(response));
    }
}
