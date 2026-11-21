package it.pagopa.pn.mandate.middleware.msclient;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.api.AooUoIdsApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.FilteredPaIdsResponseDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.api.InternalOnlyApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupStatusDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@lombok.CustomLog
public class PnExtRegPrvtClient {

    private final InternalOnlyApi internalApi;

    private final AooUoIdsApi aooUoIdsApi;

    public PnExtRegPrvtClient(InternalOnlyApi internalApi,AooUoIdsApi aooUoIdsApi) {
        this.internalApi = internalApi;
        this.aooUoIdsApi = aooUoIdsApi;
    }

    public Flux<PgGroupDto> getGroups(String id, boolean onlyActive) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Retrieving PG groups");
        return internalApi.getAllPgGroupsPrivate(id, onlyActive? PgGroupStatusDto.ACTIVE:null);
    }

    public Mono<FilteredPaIdsResponseDto> checkAooUoV2Ids(List<String> senderIdList) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Check aoo uo in senderId list with v2 api");
        return this.aooUoIdsApi.getFilteredAooUoIdV2Private(senderIdList);
    }
}
