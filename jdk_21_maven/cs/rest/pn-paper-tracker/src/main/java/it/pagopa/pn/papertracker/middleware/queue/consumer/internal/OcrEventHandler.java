package it.pagopa.pn.papertracker.middleware.queue.consumer.internal;

import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.generic.HandlersRegistry;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@CustomLog
public class OcrEventHandler {

    private final PaperTrackingsDAO paperTrackingsDAO;
    private final PaperTrackerExceptionHandler paperTrackerExceptionHandler;
    private final HandlersRegistry handlersRegistry;

    public void handleOcrMessage(OcrDataResultPayload ocrResultMessage) {
        if (Objects.isNull(ocrResultMessage) || Objects.isNull(ocrResultMessage.getCommandId())) {
            log.error("Received null payload or commandId in OcrResponse handler");
            throw new IllegalArgumentException("Payload or commandId cannot be null");
        }

        String processName = "processOcrResponseMessage";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, ocrResultMessage.getCommandId());
        log.logStartingProcess(processName);
        MDCUtils.addMDCToContextAndExecute(paperTrackingsDAO.retrieveEntityByTrackingId(TrackerUtility.getParsedOcrCommandId(ocrResultMessage.getCommandId())[0])
                        .flatMap(paperTrackings -> callOcrResponseHandler(paperTrackings, ocrResultMessage))
                        .onErrorResume(PnPaperTrackerValidationException.class, e -> paperTrackerExceptionHandler.handleInternalException(e, null))
                        .then())
                .block();
    }

    private Mono<Void> callOcrResponseHandler(PaperTrackings paperTrackings, OcrDataResultPayload ocrResultMessage) {
        HandlerContext context = buildContext(paperTrackings, ocrResultMessage);
        return handlersRegistry.handleEvent(ProductType.fromValue(paperTrackings.getProductType()), EventTypeEnum.OCR_RESPONSE_EVENT, context);
    }

    private HandlerContext buildContext(PaperTrackings paperTrackings,
                                        OcrDataResultPayload ocrResultMessage) {
        HandlerContext handlerContext = new HandlerContext();
        handlerContext.setTrackingId(paperTrackings.getTrackingId());
        handlerContext.setPaperTrackings(paperTrackings);
        handlerContext.setOcrDataResultPayload(ocrResultMessage);
        return handlerContext;
    }
}
