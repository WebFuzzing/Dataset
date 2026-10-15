package it.pagopa.pn.papertracker.service.handler_step._890;

import com.sngular.apigenerator.asyncapi.business_model.model.event.Data;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.mapper.SendEventMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG012;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG012A;

/**
 * Questo step viene invocato dai seguenti handler:
 * - StockIntermediateEventHandler (RECAG011B, RECAG005B, RECAG006B, RECAG007B, RECAG008B)
 * - Recag012EventHandler (RECAG012)
 * - OcrResponseHandler890 (RECAG012, RECAG001C, RECAG002C, RECAG003C,
 *   RECAG003F, RECAG005C, RECAG006C, RECAG007C, RECAG008C)
 *
 * Regole di esclusione:
 * Lo step non esegue alcuna azione se:
 * - Il tracking è già in stato DONE
 * - L’evento corrente è di tipo FINAL_EVENT
 *   (RECAG001C, RECAG002C, RECAG003C, RECAG003F,
 *    RECAG005C, RECAG006C, RECAG007C, RECAG008C)
 *
 * Gestione RECAG012A:
 * Il flag needToSendRECAG012A viene valorizzato a monte ed è TRUE se e solo se:
 * - Non sono arrivati tutti gli allegati obbligatori al refinement ma è arrivato il RECAG012
 * - L’handler chiamante non è OcrResponseHandler890
 *
 * Se il flag è TRUE:
 * - Viene inviato RECAG012A (status PROGRESS) solo se l’evento corrente è RECAG012
 * - Negli altri casi il flusso termina
 *
 * Gestione RECAG012 (flusso principale):
 *
 * Se needToSendRECAG012A è FALSE si distinguono due macro-scenari:
 *
 * 1) OCR DISABLED o DRY
 * - Se l’evento corrente è RECAG012 viene inviato RECAG012
 * - Se l’evento corrente è diverso viene inviato RECAG012
 *   utilizzando l’evento RECAG012 solo se già presente nel tracking
 *
 * 2) OCR RUN
 * - Se l’evento corrente è RECAG012 e tutte le risposte OCR
 *   relative agli allegati obbligatori sono arrivate, oppure non è previsto nessun allegato obbligatorio,
 *   viene inviato RECAG012
 * - Se le risposte OCR non sono complete il flusso termina
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RECAG012EventBuilder implements HandlerStep {


    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing RECAG012EventBuilder step for trackingId: {}", context.getTrackingId());

        PaperTrackings tracking = context.getPaperTrackings();
        // StatusCode dell'evento appena arrivato da pn-ec (RECAG011B o RECA00xB)
        // Oppure StatusCode dell'evento inviato all'OCR (RECAG012, RECAG00xC, RECAG00xF)
        Event currentEvent = TrackerUtility.getEventFromEventId(tracking, context.getEventId());
        String currentStatusCode = currentEvent.getStatusCode();
        EventStatusCodeEnum eventStatus = EventStatusCodeEnum.fromKey(currentStatusCode);

        // Filtro eventi RECAG001C, RECAG002C, RECAG003C, RECAG003F, RECAG005C, RECAG006C, RECAG007C, RECAG008C
        if (tracking.getState() == PaperTrackingsState.DONE || eventStatus.getCodeType() == EventTypeEnum.FINAL_EVENT) {
            log.info("Skip RECAG012 build for trackingId={} (state={}, status={})", context.getTrackingId(), tracking.getState(), currentStatusCode);
            return Mono.empty();
        }

        boolean isRecag012 = RECAG012.name().equalsIgnoreCase(currentStatusCode);
        boolean ocrDisabled = isOcrDisabledOrDry(tracking);
        boolean allOcrCompleted = TrackerUtility.isOcrResponseCompleted(context, RECAG012.name());

        // isNeedToSendRECAG012A true se è arrivato RECAG012 e non ci sono tutti gli allegati
        // false se il RECAG012 non è ancora arrivato
        // false se il RECAG012 è arrivato e ci sono tutti gli allegati
        if (context.isNeedToSendRECAG012A()) {
            return isRecag012 ? sendRecag012A(context) : Mono.empty();
        }

        Event recag012Event = findExistingRecag012(context.getPaperTrackings());

        if(recag012Event == null){
            log.info("Skip RECAG012 build, RECAG012 not found in events");
            return Mono.empty();
        }

        // Da qui in poi è arrivato il RECAG012 e ci sono tutti gli allegati required

        // OCR DISABLED / DRY
        if (ocrDisabled) {
            return sendRecag012(context, recag012Event);
        }

        // OCR RUN
        if (allOcrCompleted) {
            log.info("Check OCR responses completed: OCR RUN");
            return sendRecag012(context, recag012Event);
        }

        log.info("Skip RECAG012 build");
        return Mono.empty();
    }

    private boolean isOcrDisabledOrDry(PaperTrackings tracking) {
        OcrStatusEnum status = tracking.getValidationConfig().getOcrEnabled();
        return OcrStatusEnum.DISABLED.equals(status) || OcrStatusEnum.DRY.equals(status);
    }

    private Mono<Void> sendRecag012(HandlerContext context, Event recag012Event) {

        return SendEventMapper.createSendEventsFromEventEntity(
                        context.getTrackingId(),
                        recag012Event,
                        StatusCodeEnum.OK,
                        RECAG012.name(),
                        recag012Event.getStatusTimestamp().atOffset(ZoneOffset.UTC)
                )
                .doOnNext(context.getEventsToSend()::add)
                .doOnNext(sendEvent -> context.setFinalStatusCode(recag012Event.getStatusCode()))
                .collectList()
                .filter(sendEvents -> !CollectionUtils.isEmpty(sendEvents))
                .doOnNext(list -> updateContextPaperTrackings(context))
                .then();
    }

    private void updateContextPaperTrackings(HandlerContext context) {
        PaperTrackings paperTrackings = context.getPaperTrackings();
        ValidationFlow validationFlow = Optional.ofNullable(paperTrackings.getValidationFlow())
                .orElse(new ValidationFlow());
        validationFlow.setRefinementDematValidationTimestamp(Instant.now());
        paperTrackings.setValidationFlow(validationFlow);
        paperTrackings.setState(PaperTrackingsState.DONE);
    }

    private Mono<Void> sendRecag012A(HandlerContext context) {
        return buildReca012aEvent(context)
                .doOnNext(context.getEventsToSend()::add)
                .then();
    }

    private Flux<SendEvent> buildReca012aEvent(HandlerContext context) {
        return SendEventMapper.createSendEventsFromPaperProgressStatusEvent(context.getPaperProgressStatusEvent())
                .doOnNext(event -> {
                    event.setStatusCode(StatusCodeEnum.PROGRESS);
                    event.setStatusDetail(RECAG012A.name());
                    event.setStatusDescription(RECAG012A.name());
                });
    }

    private Event findExistingRecag012(PaperTrackings tracking) {
        return Optional.ofNullable(tracking.getEvents())
                .orElse(List.of())
                .stream()
                .filter(e -> RECAG012.name().equalsIgnoreCase(e.getStatusCode()))
                .findFirst()
                .orElse(null);
    }

}
