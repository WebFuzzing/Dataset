package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetrySenderCON996 implements HandlerStep {

    private final PaperChannelClient paperChannelClient;
    private final PcRetryService pcRetryService;

    /**
     * Step di invio della richiesta di retry al Paper Channel per tutti gli eventi CON996.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing RetrySenderCON996 step for trackingId: {}", context.getTrackingId());

        return paperChannelClient.getPcRetry(context, Boolean.TRUE)
                .doOnError(throwable -> log.error("Error retrieving retry on CON996 for trackingId: {}", context.getPaperTrackings().getTrackingId(), throwable))
                .flatMap(pcRetryResponse -> pcRetryService.handlePcRetryResponse(pcRetryResponse, Boolean.TRUE, context));
    }
}
