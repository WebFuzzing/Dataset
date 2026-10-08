package it.pagopa.pn.papertracker.middleware.msclient.impl;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.api.PaperAddressesApi;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataVaultClientImpl implements DataVaultClient {

    private final PaperAddressesApi paperAddressesApi;

    @Override
    public Mono<String> anonymizeDiscoveredAddress(String trackingId, DiscoveredAddress discoveredAddress) {
        String uuid = UUID.randomUUID().toString();
        String PREFIX_ANONYMIZED_ADDRESS_ID = "anonymized_addr_";
        String anonymizedDiscoveredAddressId = PREFIX_ANONYMIZED_ADDRESS_ID + uuid;
        return paperAddressesApi.updatePaperAddress(trackingId, anonymizedDiscoveredAddressId, toPaperAddress(discoveredAddress))
                .thenReturn(anonymizedDiscoveredAddressId);
    }

    @Override
    public Mono<PaperAddress> deAnonymizeDiscoveredAddress(String trackingId, String anonymizeDiscoveredAddress) {
        return paperAddressesApi.getPaperAddressByIds(trackingId, anonymizeDiscoveredAddress)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("No address found for the given anonymized ID: " + anonymizeDiscoveredAddress)));
    }

    private PaperAddress toPaperAddress(DiscoveredAddress discoveredAddress) {
        return new PaperAddress()
                .name(discoveredAddress.getName())
                .nameRow2(discoveredAddress.getNameRow2())
                .address(discoveredAddress.getAddress())
                .addressRow2(discoveredAddress.getAddressRow2())
                .cap(discoveredAddress.getCap())
                .city(discoveredAddress.getCity())
                .city2(discoveredAddress.getCity2())
                .pr(discoveredAddress.getPr())
                .country(discoveredAddress.getCountry());
    }
}
