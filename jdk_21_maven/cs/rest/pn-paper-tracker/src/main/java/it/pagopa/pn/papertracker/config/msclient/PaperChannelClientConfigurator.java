package it.pagopa.pn.papertracker.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.ApiClient;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.api.PcRetryApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaperChannelClientConfigurator extends CommonBaseClient {

    @Bean
    public PcRetryApi pcRetryApi(PnPaperTrackerConfigs cfg) {
        return new PcRetryApi(getNewApiClient(cfg));
    }

    @NotNull
    private ApiClient getNewApiClient(PnPaperTrackerConfigs cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getPaperChannelBaseUrl() );
        return newApiClient;
    }
}
