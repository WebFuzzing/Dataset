package it.pagopa.pn.papertracker.middleware.msclient;

import reactor.core.publisher.Mono;

public interface SafeStorageClient {

    Mono<String> getSafeStoragePresignedUrl(String safeStorageUri);
}
