package it.pagopa.pn.papertracker.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.ApiClient;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.api.PaperAddressesApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataVaultClientConfigurator extends CommonBaseClient {

    @Bean
    public PaperAddressesApi paperAddressesApi(PnPaperTrackerConfigs cfg) {
        return new PaperAddressesApi(getNewApiClient(cfg));
    }

    @NotNull
    private ApiClient getNewApiClient(PnPaperTrackerConfigs cfg) {
        ApiClient newApiClient = new ApiClient(initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(cfg.getDataVaultBaseUrl());
        return newApiClient;
    }
}
