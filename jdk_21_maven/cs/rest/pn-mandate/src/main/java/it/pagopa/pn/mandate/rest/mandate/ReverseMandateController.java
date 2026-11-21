package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDtoRequest;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDtoResponse;
import it.pagopa.pn.mandate.reverse.generated.openapi.server.v1.api.MandateReverseServiceApi;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@lombok.CustomLog
public class ReverseMandateController implements MandateReverseServiceApi {

    private final MandateService mandateService;

    public ReverseMandateController(MandateService mandateService) {
        this.mandateService = mandateService;
    }


    @Override
    public Mono<ResponseEntity<MandateDtoResponse>> createReverseMandate(String xPagopaPnUid, String xPagopaPnCxId, CxTypeAuthFleet xPagopaPnCxType, String xPagopaPnSrcCh, List<String> xPagopaPnCxGroups, String xPagopaPnCxRole, Mono<MandateDtoRequest> mandateDtoRequest, final ServerWebExchange exchange) {
        return mandateDtoRequest.flatMap(request -> mandateService.createReverseMandate(request, xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxType, xPagopaPnSrcCh, xPagopaPnCxGroups, xPagopaPnCxRole)
                .map(m -> {
                    MandateDtoResponse response = new MandateDtoResponse();
                    response.setMandateId(m);
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                }));
    }
}
