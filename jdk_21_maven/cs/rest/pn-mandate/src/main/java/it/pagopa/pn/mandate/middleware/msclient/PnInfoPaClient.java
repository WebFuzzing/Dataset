package it.pagopa.pn.mandate.middleware.msclient;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.api.InfoPaApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.PaSummaryDto;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@lombok.CustomLog
public class PnInfoPaClient {

    private final InfoPaApi infoPaApi;

    public PnInfoPaClient(InfoPaApi infoPaApi) {
        this.infoPaApi = infoPaApi;
    }


    public Flux<PaSummaryDto> getManyPa(List<String> paIds) {
        log.logInvokingExternalService(PnLogger.EXTERNAL_SERVICES.PN_EXTERNAL_REGISTRIES, "Retrieving PAs summary infos");
        return infoPaApi.getManyPa(paIds);
    }
}
