package it.pagopa.pn.papertracker.config.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.generated.openapi.msclient.safestorage.ApiClient;
import it.pagopa.pn.papertracker.generated.openapi.msclient.safestorage.api.FileDownloadApi;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SafeStorageClientConfigurator extends CommonBaseClient {

    @Bean
    public FileDownloadApi getSafeStorageClient (PnPaperTrackerConfigs paperTrackerConfigs){
        return new FileDownloadApi(getNewApiClient(paperTrackerConfigs));
    }

    @NotNull
    private ApiClient getNewApiClient(PnPaperTrackerConfigs cfg) {
        ApiClient newApiClient = new ApiClient( initWebClient(ApiClient.buildWebClientBuilder()) );
        newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );
        return newApiClient;
    }
}
