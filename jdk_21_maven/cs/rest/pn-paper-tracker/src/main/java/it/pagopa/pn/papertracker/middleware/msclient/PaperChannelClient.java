package it.pagopa.pn.papertracker.middleware.msclient;

import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.model.HandlerContext;
import reactor.core.publisher.Mono;

public interface PaperChannelClient {

    Mono<PcRetryResponse> getPcRetry(HandlerContext context, Boolean checkApplyRasterization);
}
