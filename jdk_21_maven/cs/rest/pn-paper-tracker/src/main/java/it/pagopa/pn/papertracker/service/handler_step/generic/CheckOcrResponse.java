package it.pagopa.pn.papertracker.service.handler_step.generic;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sngular.apigenerator.asyncapi.business_model.model.event.Data;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckOcrResponse implements HandlerStep {

    private final PaperTrackingsDAO paperTrackingsDAO;
    private final ObjectMapper objectMapper;

    /**
     * Step che effettua un controllo sul messaggio ricevuto dal servizio OCR.<br>
     * Se lo stato della notifica è AWAITING_OCR, il flusso prosegue, altrimenti, viene
     * generato un errore.
     * Viene aggiornato il contesto con l'eventId e dryRun recuperati dall'evento finale.<br>
     * Se lo tato di validazione dell'OCR è <br>
     * - OK: il flusso prosegue con gli step successivi <br>
     * - KO: viene generato un errore ed il flusso viene interrotto <br>
     * - PENDING: il flusso viene interrotto <br>
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing CheckOcrResponse step for trackingId: {}", context.getTrackingId());

        OcrDataResultPayload ocrResultMessage = context.getOcrDataResultPayload();

        return Mono.just(context.getPaperTrackings())
                .flatMap(paperTrackings -> {
                    Event event = TrackerUtility.extractFinalEventFromOcr(ocrResultMessage.getCommandId(), paperTrackings);
                    if (TrackerUtility.isInInvalidStateForOcr(paperTrackings, event.getStatusCode())
                            && OcrStatusEnum.RUN.equals(paperTrackings.getValidationConfig().getOcrEnabled())) {
                        return handleFinalStateError(event, paperTrackings, ocrResultMessage);
                    }
                    updateContext(context, event);
                    Data.ValidationStatus validationStatus = ocrResultMessage.getData().getValidationStatus();
                    return handleValidationStatus(validationStatus, paperTrackings, event, ocrResultMessage, context);
                })
                .then();
    }

    private void updateContext(HandlerContext context, Event event) {
        context.setEventId(event.getId());
        context.setDryRunEnabled(event.getDryRun());
    }

    private Mono<Void> handleFinalStateError(Event event, PaperTrackings paperTrackings, OcrDataResultPayload ocrResultMessage) {

        return Mono.error(new PnPaperTrackerValidationException(
                "Error in OCR validation for requestId: " + ocrResultMessage.getCommandId(),
                PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                        paperTrackings, event.getStatusCode(), ErrorCategory.OCR_VALIDATION,
                        ErrorCause.OCR_DUPLICATED_EVENT, "CommandId: " + ocrResultMessage.getCommandId(),
                        Map.of("ocrDataResultPayload", transformToMap(ocrResultMessage.getData())),
                        FlowThrow.DEMAT_VALIDATION, ErrorType.WARNING, event.getId()
                )
        ));
    }

    private Map<String,Object> transformToMap(Data data) {
        return  objectMapper.convertValue(data, new TypeReference<>() {});
    }


    private Mono<Void> handleValidationStatus(Data.ValidationStatus status, PaperTrackings paperTrackings,
                                              Event event, OcrDataResultPayload ocrResultMessage, HandlerContext context) {
        String[] parsedOcrCommandId = TrackerUtility.getParsedOcrCommandId(ocrResultMessage.getCommandId());
        String docType = parsedOcrCommandId[2];
        String trackingId = parsedOcrCommandId[0];

        Integer index = TrackerUtility.getOcrRequestIndexByEventIdAndDocType(paperTrackings, context.getEventId(), docType);

        return switch (status) {
            case KO -> {
                log.info("OCR validation KO for requestId: {}", ocrResultMessage.getCommandId());
                yield paperTrackingsDAO.updateOcrRequests(index, trackingId, Data.ValidationStatus.KO)
                        .doOnNext(context::setPaperTrackings)
                        .then(Mono.error(new PnPaperTrackerValidationException(
                                "Error in OCR validation for requestId: " + ocrResultMessage.getCommandId(),
                                PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                                        paperTrackings, event.getStatusCode(), ErrorCategory.OCR_VALIDATION,
                                        ErrorCause.OCR_KO, ocrResultMessage.getData().getDescription(),
                                        Map.of("ocrDataResultPayload", transformToMap(ocrResultMessage.getData())),
                                        FlowThrow.DEMAT_VALIDATION,
                                        OcrStatusEnum.RUN.equals(paperTrackings.getValidationConfig().getOcrEnabled())? ErrorType.ERROR : ErrorType.WARNING, event.getId()
                                )
                        )));
            }

            case OK -> {
                log.info("OCR validation successful for requestId: {}", ocrResultMessage.getCommandId());
                yield handleOkStatus(paperTrackings, event, ocrResultMessage, context, index);
            }

            case PENDING -> {
                log.info("OCR validation pending for requestId: {}", ocrResultMessage.getCommandId());
                yield paperTrackingsDAO.updateOcrRequests(index, trackingId, Data.ValidationStatus.PENDING)
                        .doOnNext(unused -> context.setStopExecution(true))
                        .then();
            }
        };
    }

    private Mono<Void> handleOkStatus(PaperTrackings tracking, Event event, OcrDataResultPayload payload, HandlerContext context, Integer index) {
        String trackingId = tracking.getTrackingId();
        return paperTrackingsDAO.updateOcrRequests(index, trackingId, Data.ValidationStatus.OK)
                .doOnNext(context::setPaperTrackings)
                .flatMap(paperTrackings -> {
                    if(!OcrStatusEnum.RUN.equals(paperTrackings.getValidationConfig().getOcrEnabled())){
                        log.info("OCR validation OK but OCR is not enabled for trackingId: {}, stopping execution", context.getTrackingId());
                        context.setStopExecution(true);
                        return Mono.empty();
                    }
                    log.info("OCR validation OK, checking if all OCR validations are completed for trackingId: {}", context.getTrackingId());
                    return Mono.just(TrackerUtility.isOcrResponseCompleted(context, event.getStatusCode()));
                })
                .flatMap(ocrResponseCompleted -> {
                    if (ocrResponseCompleted) {
                        log.info("All OCR validations completed for trackingId: {}", context.getTrackingId());
                        return paperTrackingsDAO.updateItem(trackingId, getPaperTrackingsToUpdate(event.getStatusCode(), payload))
                                .doOnNext(context::setPaperTrackings)
                                .then();
                    }
                    log.info("OCR validation pending for other requests, trackingId: {}", context.getTrackingId());
                    context.setStopExecution(true);
                    return Mono.empty();
                });
    }

    private PaperTrackings getPaperTrackingsToUpdate(String statusCode, OcrDataResultPayload payload) {
        PaperTrackings paperTrackings = new PaperTrackings();
        TrackerUtility.setDematValidationTimestamp(paperTrackings, statusCode);
        PaperStatus paperStatus = new PaperStatus();
        paperStatus.setPredictedRefinementType(Objects.nonNull(payload.getData().getPredictedRefinementType()) ?
                payload.getData().getPredictedRefinementType().getValue() : null);
        paperTrackings.setPaperStatus(paperStatus);
        return paperTrackings;
    }

}
