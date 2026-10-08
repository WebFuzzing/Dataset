package it.pagopa.pn.papertracker.middleware.msclient;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import reactor.core.publisher.Mono;

public interface DataVaultClient {

    Mono<String> anonymizeDiscoveredAddress(String trackingId, DiscoveredAddress discoveredAddress);

    Mono<PaperAddress> deAnonymizeDiscoveredAddress(String trackingId, String anonymizeDiscoveredAddress);
}
