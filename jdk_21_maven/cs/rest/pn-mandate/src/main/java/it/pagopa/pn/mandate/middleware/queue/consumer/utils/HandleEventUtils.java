package it.pagopa.pn.mandate.middleware.queue.consumer.utils;

import it.pagopa.pn.api.dto.events.StandardEventHeader;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import org.springframework.messaging.MessageHeaders;

import java.time.Instant;

import static it.pagopa.pn.api.dto.events.StandardEventHeader.*;
import static it.pagopa.pn.mandate.exceptions.PnMandateExceptionCodes.ERROR_CODE_MANDATE_INVALID_MESSAGE_HEADERS;

@lombok.CustomLog
public class HandleEventUtils {
    private HandleEventUtils() {
    }

    //per versione asincrona
//    public static Mono<Void> handleException(Flux<Message<PnMandateExpiredEvent.Payload>> messageMono, Throwable t) {
//        return messageMono
//                .doOnNext(message -> handleException(message.getHeaders(), t))
//                .then(Mono.error(t));
//    }

    public static void handleException(MessageHeaders headers, Throwable t) {
        if (headers != null) {
            StandardEventHeader standardEventHeader = mapStandardEventHeader(headers);
            log.error("Generic exception for iun={} ex={}", standardEventHeader.getIun(), t);
        } else {
            log.error("Generic exception ex ", t);
        }
    }

    public static StandardEventHeader mapStandardEventHeader(MessageHeaders headers) {
        if (headers != null) {
            return StandardEventHeader.builder()
                    .eventId((String) headers.get(PN_EVENT_HEADER_EVENT_ID))
                    .eventType((String) headers.get(PN_EVENT_HEADER_EVENT_TYPE))
                    .createdAt(mapInstant(headers.get(PN_EVENT_HEADER_CREATED_AT)))
                    .publisher((String) headers.get(PN_EVENT_HEADER_PUBLISHER))
                    .build();
        } else {
            String msg = "Headers cannot be null in mapStandardEventHeader";
            log.error(msg);
            throw new PnInternalException(msg, ERROR_CODE_MANDATE_INVALID_MESSAGE_HEADERS);
        }
    }

    private static Instant mapInstant(Object createdAt) {
        return createdAt != null ? Instant.parse((CharSequence) createdAt) : null;
    }

}
