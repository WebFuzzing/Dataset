package it.pagopa.pn.papertracker.middleware.queue.consumer.internal;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.service.SourceQueueProxyService;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;
import java.util.Objects;

/**
 * Handler responsabile della gestione degli eventi provenienti da pn-external-channel (pn-ec).
 * <p>
 * In fase di inizializzazione di una spedizione su pn-paper-tracker, viene determinata e persistita
 * la modalità operativa (DRY o RUN) sull'entità della tabella {pn-PaperTrackings.
 * </p>
 *
 * <p>
 * Conseguentemente, alla ricezione di un evento da pn-ec, pn-paper-tracker determina la modalità
 * operativa recuperando le informazioni di tracking dalla tabella pn-PaperTrackings
 * e gestisce ciascun evento in base alla logica definita.
 * </p>
 *
 * <ul>
 *   <li>
 *     <b>Spedizione non presente su pn-PaperTrackings</b><br>
 *     L'evento viene inoltrato direttamente a pn-paper-channel.
 *   </li>
 *   <li>
 *     <b>Spedizione presente su pn-PaperTrackings e modalità DRY</b><br>
 *     L'evento viene inoltrato a pn-paper-channel (se non duplicato) e, in parallelo, processato internamente
 *     da pn-paper-tracker, con persistenza dell'output su tabella a fini di verifica.
 *   </li>
 *   <li>
 *     <b>Spedizione presente su pn-PaperTrackings e modalità RUN</b><br>
 *     L'evento viene processato internamente da pn-paper-tracker e l'output
 *     viene inviato a pn-delivery-push.
 *   </li>
 * </ul>
 *
 * La gestione del flusso operativo effettivo è demandata al {@link SourceQueueProxyService}.
 */
@Component
@RequiredArgsConstructor
@CustomLog
public class ExternalChannelSourceEventsHandler {
    private final SourceQueueProxyService sourceQueueProxyService;

    public void handleExternalChannelMessage(
            SingleStatusUpdate message,
            Map<String, MessageAttributeValue> messageAttributes) {
        if (Objects.isNull(message) || Objects.isNull(message.getAnalogMail())) {
            log.error("Received null payload or analogMail in ExternalChannelHandler");
            throw new IllegalArgumentException("Payload or analogMail cannot be null");
        }

        String processName = "processExternalChannelSourceMessage";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, message.getAnalogMail().getRequestId());
        log.logStartingProcess(processName);

        MDCUtils.addMDCToContextAndExecute(sourceQueueProxyService.handleExternalChannelMessage(message, messageAttributes)
                        .doOnSuccess(unused -> log.logEndingProcess(processName)))
                .block();
    }
}
