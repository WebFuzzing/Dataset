package it.pagopa.pn.papertracker.middleware.msclient.impl;

import it.pagopa.pn.commons.log.PnLogger;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.safestorage.api.FileDownloadApi;
import it.pagopa.pn.papertracker.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.papertracker.utils.AttachmentsConfigUtils;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
@CustomLog
public class SafeStorageClientImpl implements SafeStorageClient {

    private final FileDownloadApi fileDownloadApi;
    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;

    /**
     * Retrieves the presigned download URL from the SafeStorage.
     *
     * @param fileKey the object key
     * @return Mono containing the presigned URL
     */
    public Mono<String> getSafeStoragePresignedUrl(String fileKey) {
        final String PN_SAFE_STORAGE_DESCRIPTION = "Safe Storage getFile";
        log.logInvokingAsyncExternalService(PnLogger.EXTERNAL_SERVICES.PN_SAFE_STORAGE, PN_SAFE_STORAGE_DESCRIPTION, null);
        String reqFileKey = fileKey;
        log.info("Getting file with {} key", fileKey);
        fileKey = AttachmentsConfigUtils.cleanFileKey(fileKey);
        log.debug("Req params : {}", fileKey);

        return fileDownloadApi.getFile(fileKey, this.pnPaperTrackerConfigs.getSafeStorageCxId(), false)
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)).filter(throwable -> throwable instanceof TimeoutException || throwable instanceof ConnectException))
                .flatMap(response -> {
                    if(response.getDownload() != null && response.getDownload().getRetryAfter() != null) {
                        return Mono.error(new PaperTrackerException(String.valueOf(response.getDownload().getRetryAfter())));
                    }
                    if (response.getDownload() == null || response.getDownload().getUrl() == null) {
                        log.error("File not found in Safe Storage for key: {}", reqFileKey);
                        return Mono.error(new PaperTrackerException("File not found in Safe Storage"));
                    }
                    return Mono.just(response.getDownload().getUrl());
                })
                .onErrorResume(WebClientResponseException.class, ex -> {
                    log.error(ex.getResponseBodyAsString());
                    return Mono.error(ex);
                });
    }
}
