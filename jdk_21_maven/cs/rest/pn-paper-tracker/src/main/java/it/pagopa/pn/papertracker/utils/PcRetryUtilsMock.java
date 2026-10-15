package it.pagopa.pn.papertracker.utils;

import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.CON996;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class PcRetryUtilsMock {

    public static final String RETRY = ".PCRETRY_";
    public static final String PCRETRY = "PCRETRY_";


    static public Mono<PcRetryResponse> getPcRetryPaperMock(PaperTrackings paperTrackings, int maxRetry, String statusCode) {
        PcRetryResponse pcRetryResponse = new PcRetryResponse();
        pcRetryResponse.setParentRequestId(paperTrackings.getTrackingId());
        pcRetryResponse.setDeliveryDriverId(paperTrackings.getUnifiedDeliveryDriver());

        if (hasOtherAttempt(paperTrackings.getTrackingId(), statusCode.equalsIgnoreCase(CON996.name()) ? 0 : maxRetry)) {
            pcRetryResponse.setRetryFound(true);
            String newRequestId = setRetryRequestId(paperTrackings.getTrackingId());
            setRetryRequestIdAndPcRetry(pcRetryResponse, newRequestId);
            return Mono.just(pcRetryResponse);
        }

        pcRetryResponse.setRetryFound(false);
        return Mono.just(pcRetryResponse);
    }

    static public boolean hasOtherAttempt(String requestId, int maxRetry) {
        return maxRetry == -1 || maxRetry >= getRetryAttempt(requestId);
    }

    static public int getRetryAttempt(String requestId) {
        int retry = 0;
        if (requestId.contains(RETRY)) {
            retry = Integer.parseInt(requestId.substring(requestId.lastIndexOf("_")+1));
        }
        return retry;
    }

    static public String setRetryRequestId(String requestId) {
        if (requestId.contains(RETRY)) {
            String prefix = requestId.substring(0, requestId.indexOf(RETRY));
            String attempt = String.valueOf(getRetryAttempt(requestId) + 1);
            requestId = prefix.concat(RETRY).concat(attempt);
        }
        return requestId;
    }

    static public void setRetryRequestIdAndPcRetry(PcRetryResponse pcRetryResponse, String newRequestId) {
        String suffix = newRequestId.substring(newRequestId.indexOf(PCRETRY), newRequestId.length());
        pcRetryResponse.setPcRetry(suffix);
        pcRetryResponse.setRequestId(newRequestId);
    }
}
