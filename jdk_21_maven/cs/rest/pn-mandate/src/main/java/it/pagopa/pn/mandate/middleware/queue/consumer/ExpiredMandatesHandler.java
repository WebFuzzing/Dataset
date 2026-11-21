package it.pagopa.pn.mandate.middleware.queue.consumer;

import io.awspring.cloud.sqs.annotation.SqsListener;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.mandate.middleware.queue.consumer.event.PnMandateExpiredEvent;
import it.pagopa.pn.mandate.middleware.queue.consumer.utils.HandleEventUtils;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import static it.pagopa.pn.mandate.middleware.queue.consumer.utils.ConsumerUtils.setMdc;

@Component
@lombok.CustomLog
@RequiredArgsConstructor
public class ExpiredMandatesHandler {
    private final MandateService mandateService;

    @SqsListener(value = "${pn.mandate.topics.mandate-inputs}")
    public void pnMandateExpiredMandatesConsumer(Message<PnMandateExpiredEvent.Payload> message) {
        setMdc(message);
        String process = "expired mandate cleanup";
        try {
            log.logStartingProcess(process);
            PnMandateExpiredEvent.Payload payload = message.getPayload();
            log.debug("pnMandateExpiredMandatesConsumer, payload {}", payload);
            MDC.put(MDCUtils.MDC_PN_MANDATEID_KEY, payload.getMandateId());
            MDCUtils.addMDCToContextAndExecute(mandateService.expireMandate(payload.getMandateId(),
                    payload.getDelegatorInternalUserid(), payload.getDelegatorUserid(), payload.getDelegatorCxType())).block();
            log.logEndingProcess(process);
        } catch (Exception ex) {
            HandleEventUtils.handleException(message.getHeaders(), ex);
            log.logEndingProcess(process, false, ex.getMessage(), ex);
            throw ex;
        }
    }

}
