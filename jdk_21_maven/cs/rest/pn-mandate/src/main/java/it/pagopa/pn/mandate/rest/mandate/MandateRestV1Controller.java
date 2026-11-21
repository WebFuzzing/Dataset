package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.mandate.generated.openapi.server.v1.api.MandateServiceApi;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@lombok.CustomLog
public class MandateRestV1Controller  implements MandateServiceApi {

    private final MandateService mandateService;

    public MandateRestV1Controller(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @Override
    public Mono<ResponseEntity<Void>> acceptMandate(String xPagopaPnCxId,
                                                    CxTypeAuthFleet xPagopaPnCxType,
                                                    String mandateId,
                                                    List<String> cxGroups,
                                                    String cxRole,
                                                    Mono<AcceptRequestDto> acceptRequestDto,
                                                    ServerWebExchange exchange) {


        MDC.put(MDCUtils.MDC_PN_MANDATEID_KEY, mandateId);

        return MDCUtils.addMDCToContextAndExecute(mandateService.acceptMandate(mandateId, acceptRequestDto, xPagopaPnCxId, xPagopaPnCxType, cxGroups, cxRole)
                    .thenReturn(ResponseEntity.noContent().build()));

    }

    @Override
    public Mono<ResponseEntity<Void>> updateMandate(String xPagopaPnCxId,
                                                    CxTypeAuthFleet xPagopaPnCxType,
                                                    String mandateId,
                                                    List<String> xPagopaPnCxGroups,
                                                    String xPagopaPnCxRole,
                                                    Mono<UpdateRequestDto> updateRequestDto,
                                                    ServerWebExchange exchange) {
        return mandateService.updateMandate(xPagopaPnCxId, xPagopaPnCxType, mandateId, xPagopaPnCxGroups, xPagopaPnCxRole, updateRequestDto)
            .then(Mono.just(ResponseEntity.noContent().build()));
    }

    @Override
    public Mono<ResponseEntity<MandateCountsDto>> countMandatesByDelegate(String xPagopaPnCxId,
                                                                          CxTypeAuthFleet xPagopaPnCxType,
                                                                          List<String> cxGroups,
                                                                          String cxRole,
                                                                          String status,
                                                                          ServerWebExchange exchange) {


        return mandateService.countMandatesByDelegate(status, xPagopaPnCxId, xPagopaPnCxType, cxGroups, cxRole)
                    .map(m -> ResponseEntity.status(HttpStatus.OK).body(m));
    }

    @Override
    public Mono<ResponseEntity<MandateDto>> createMandate(String xPagopaPnUid,
                                                          String xPagopaPnCxId,
                                                          CxTypeAuthFleet xPagopaPnCxType,
                                                          String xPagopaPnSrcCh,
                                                          List<String> groups,
                                                          String role,
                                                          Mono<MandateDto> mandateDto,
                                                          final ServerWebExchange exchange) {

        return mandateService.createMandate(mandateDto, xPagopaPnUid, xPagopaPnCxId, xPagopaPnCxType, xPagopaPnSrcCh, groups, role)
                    .map(m ->  ResponseEntity.status(HttpStatus.CREATED).body(m));
    }                       

    @Override
    public Mono<ResponseEntity<Flux<MandateDto>>> listMandatesByDelegate1(String xPagopaPnCxId,
                                                                          CxTypeAuthFleet xPagopaPnCxType,
                                                                          List<String> xPagopaPnCxGroups,
                                                                          String xPagopaPnCxRole,
                                                                          String status,
                                                                          ServerWebExchange exchange) {

        return mandateService.listMandatesByDelegate(status, xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxGroups, xPagopaPnCxRole)
                .collectList()
                .map(m -> ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(m)));
    }

    @Override
    public Mono<ResponseEntity<SearchMandateResponseDto>> searchMandatesByDelegate(String xPagopaPnCxId,
                                                                                   CxTypeAuthFleet xPagopaPnCxType,
                                                                                   Integer size,
                                                                                   List<String> xPagopaPnCxGroups,
                                                                                   String xPagopaPnCxRole,
                                                                                   String nextPageKey,
                                                                                   Mono<SearchMandateRequestDto> searchMandateRequestDto,
                                                                                   final ServerWebExchange exchange) {

        return mandateService.searchByDelegate(searchMandateRequestDto, size, nextPageKey, xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxGroups, xPagopaPnCxRole)
                .map(m -> ResponseEntity.status(HttpStatus.OK).body(m));
    }

    @Override
    public Mono<ResponseEntity<Flux<MandateDto>>> listMandatesByDelegator1(String xPagopaPnCxId,
                                                                           CxTypeAuthFleet xPagopaPnCxType,
                                                                           List<String> xPagopaPnCxGroups,
                                                                           String xPagopaPnCxRole,
                                                                           ServerWebExchange exchange) {

        return  mandateService.listMandatesByDelegator(xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxGroups, xPagopaPnCxRole)
                .collectList()
                .map(m -> ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(m)));
    }

    @Override
    public Mono<ResponseEntity<Void>> rejectMandate(String xPagopaPnCxId,
                                                    CxTypeAuthFleet xPagopaPnCxType,
                                                    String mandateId,
                                                    List<String> xPagopaPnCxGroups,
                                                    String xPagopaPnCxRole,
                                                    ServerWebExchange exchange) {

        MDC.put( MDCUtils.MDC_PN_MANDATEID_KEY, mandateId);
        return MDCUtils.addMDCToContextAndExecute(mandateService.rejectMandate(mandateId, xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, xPagopaPnCxGroups)
                .thenReturn(ResponseEntity.noContent().build()));
    }

    @Override
    public Mono<ResponseEntity<Void>> revokeMandate(String xPagopaPnCxId,
                                                    CxTypeAuthFleet xPagopaPnCxType,
                                                    String mandateId,
                                                    List<String> xPagopaPnCxGroups,
                                                    String xPagopaPnCxRole,
                                                    ServerWebExchange exchange) {


        MDC.put( MDCUtils.MDC_PN_MANDATEID_KEY, mandateId);
        return MDCUtils.addMDCToContextAndExecute(mandateService.revokeMandate(mandateId, xPagopaPnCxId, xPagopaPnCxType, xPagopaPnCxRole, xPagopaPnCxGroups)
                .thenReturn(ResponseEntity.noContent().build()));

    }


}
