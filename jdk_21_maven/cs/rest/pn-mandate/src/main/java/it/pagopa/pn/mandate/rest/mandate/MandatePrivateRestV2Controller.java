package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.mandate.generated.openapi.server.v1.api.MandatePrivateServiceV2Api;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.InternalMandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.services.mandate.v1.MandatePrivateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;
import java.util.List;

@RestController
@lombok.CustomLog
public class MandatePrivateRestV2Controller implements MandatePrivateServiceV2Api {

    MandatePrivateService mandateService;

    public MandatePrivateRestV2Controller(MandatePrivateService mandateService) {
        this.mandateService = mandateService;
    }

    @Override
    public Mono<ResponseEntity<Flux<InternalMandateDto>>> listMandatesByDelegateV2(String internaluserId,
                                                                                   CxTypeAuthFleet xPagopaPnCxType,
                                                                                   String mandateId,
                                                                                   List<String> xPagopaPnCxGroups,
                                                                                   Instant notificationSentAt,
                                                                                   String iun,
                                                                                   String rootSenderId,
                                                                                   final ServerWebExchange exchange) {
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(internaluserId)
                .mandateId(mandateId)
                .iun(iun)
                .rootSenderId(rootSenderId)
                .notificationSentAt(notificationSentAt)
                .groups(xPagopaPnCxGroups)
                .cxType(xPagopaPnCxType)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE)) // nelle invocazioni tra servizi mi interessano SEMPRE solo le deleghe ATTIVE
                .build();
        return mandateService.listMandatesByDelegateV2(inputSearchMandateDto)
                .collectList()
                .map(m -> ResponseEntity.status(HttpStatus.OK).body(Flux.fromIterable(m)));
    }
}
