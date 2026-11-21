package it.pagopa.pn.mandate.middleware.msclient;

import it.pagopa.pn.commons.pnclients.CommonBaseClient;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.ApiClient;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.api.MandatesApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.api.InternalOnlyApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.api.AooUoIdsApi;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.api.InfoPaApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MsClientConfig extends CommonBaseClient {

    @Bean
    public RecipientsApi getRecipientsApi(PnMandateConfig mandateConfig) {
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(mandateConfig.getClientDatavaultBasepath());
        return new RecipientsApi(newApiClient);
    }

    @Bean
    public MandatesApi getMandatesApi(PnMandateConfig mandateConfig) {
        ApiClient newApiClient = new ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        newApiClient.setBasePath(mandateConfig.getClientDatavaultBasepath());
        return new MandatesApi(newApiClient);
    }

    @Bean
    public it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.api.InternalOnlyApi getInternalOnlyApi(PnMandateConfig mandateConfig) {
        it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.ApiClient apiClient = new it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.ApiClient(super.initWebClient(it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(mandateConfig.getClientExtregBasepath());
        return new it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.api.InternalOnlyApi(apiClient);
    }

    @Bean
    public AooUoIdsApi getAooUoIdsApi(PnMandateConfig mandateConfig) {
        it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.ApiClient apiClient =
                new it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(mandateConfig.getClientExtregBasepath());
        return new AooUoIdsApi(apiClient);
    }

    @Bean
    public InfoPaApi getInfoPaApi(PnMandateConfig mandateConfig) {
        it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.ApiClient apiClient = new it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.ApiClient(super.initWebClient(it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(mandateConfig.getClientExtregBasepath());
        return new InfoPaApi(apiClient);
    }

    @Bean
    public InternalOnlyApi getInternalOnlyApiDelivery(PnMandateConfig mandateConfig){
        it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.ApiClient apiClient=
                new it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.ApiClient(super.initWebClient(ApiClient.buildWebClientBuilder()));
        apiClient.setBasePath(mandateConfig.getClientDeliveryBasepath());
        return new InternalOnlyApi(apiClient);
    }


}
