package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.mapper.SendEventMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericFinalEventBuilder;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;

@Component
@Slf4j
public class FinalEventBuilderAr extends GenericFinalEventBuilder implements HandlerStep {

    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;
    private final PaperTrackingsDAO paperTrackingsDAO;

    public FinalEventBuilderAr(PnPaperTrackerConfigs pnPaperTrackerConfigs, DataVaultClient dataVaultClient, PaperTrackingsDAO paperTrackingsDAO) {
        super(dataVaultClient, paperTrackingsDAO);
        this.pnPaperTrackerConfigs = pnPaperTrackerConfigs;
        this.paperTrackingsDAO = paperTrackingsDAO;
    }

    /**
     * Step che elabora l'evento finale in base alla logica di business definita.
     * Se l'evento finale non è uno stato di giacenza, viene semplicemente aggiunto alla lista degli eventi da inviare.
     * Se l'evento finale è uno stato di giacenza, viene verificata la differenza tra le date degli eventi RECRN010 e RECRN00XA.
     * In base a questa differenza e alla configurazione, viene deciso se aggiungere l'evento finale e un evento PNRN012 alla lista degli eventi da inviare,
     * oppure se lanciare un'eccezione in caso di errore di giacenza.
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing FinalEventBuilderAr step for trackingId: {}", context.getTrackingId());

        return Mono.just(TrackerUtility.extractEventFromContext(context))
                .doOnNext(event -> context.setFinalStatusCode(event.getStatusCode()))
                .flatMap(event -> handleFinalEvent(context, event))
                .thenReturn(context)
                .map(ctx -> paperTrackingsDAO.updateItem(ctx.getPaperTrackings().getTrackingId(), getPaperTrackingsToUpdate()))
                .then();
    }

    private Mono<Void> handleFinalEvent(HandlerContext context, Event finalEvent) {
        PaperTrackings paperTrackings = context.getPaperTrackings();
        String statusCode = finalEvent.getStatusCode();
        if (!isStockStatus(statusCode)) {
            String eventStatus = TrackerUtility.evaluateStatusCodeAndRetrieveStatus(RECRN002C.name(), statusCode, context.getPaperTrackings()).name();
            return addEventToSend(context, finalEvent, eventStatus);
        }

        List<Event> validatedEvents = TrackerUtility.validatedEvents(
                paperTrackings.getPaperStatus().getValidatedEvents(),
                paperTrackings.getEvents()
        );
        EventStatusCodeEnum configEnum = getRECRN00XA(statusCode);
        Event eventRECRN00XA = getEvent(validatedEvents, configEnum);
        Event eventRECRN010 = getEvent(validatedEvents, RECRN010);

        boolean differenceGreater = isDifferenceGreater(statusCode, eventRECRN010, eventRECRN00XA);
        return differenceGreater
                ? prepareFinalEventAndPNRN012toSend(context, finalEvent, eventRECRN010)
                : handleNoDifferenceGreater(context, paperTrackings, statusCode, finalEvent, eventRECRN00XA, eventRECRN010);
    }

    private Mono<Void> handleNoDifferenceGreater(HandlerContext context,
                                                 PaperTrackings paperTrackings,
                                                 String statusCode,
                                                 Event finalEvent,
                                                 Event eventRECRN00XA,
                                                 Event eventRECRN010) {
        if (RECRN005C.name().equals(statusCode)) {
            log.error("The difference between RECRN005A and RECRN010 is greater than the configured duration, throwing exception");
            return Mono.error(new PnPaperTrackerValidationException(
                    "The difference between RECRN005A and RECRN010 is greater than the configured duration",
                    PaperTrackingsErrorsMapper.buildPaperTrackingsError(paperTrackings, finalEvent.getStatusCode(), ErrorCategory.RENDICONTAZIONE_SCARTATA, ErrorCause.GIACENZA_DATE_ERROR,
                            String.format("RECRN005A getStatusTimestamp: %s, RECRN010 getStatusTimestamp: %s", eventRECRN00XA.getStatusTimestamp(), eventRECRN010.getStatusTimestamp()),
                            Map.of("recrn005aTimestamp", eventRECRN00XA.getStatusTimestamp().toString(),
                                    "recrn010Timestamp", eventRECRN010.getStatusTimestamp().toString()
                            ),
                            FlowThrow.FINAL_EVENT_BUILDING, ErrorType.ERROR, finalEvent.getId())));
        }
        return addEventToSend(context, finalEvent, EventStatusCodeEnum.fromKey(statusCode).getStatus().name());
    }

    private boolean isStockStatus(String status) {
        return RECRN003C.name().equals(status) ||
                RECRN004C.name().equals(status) ||
                RECRN005C.name().equals(status);
    }

    private boolean isDifferenceGreater(String statusCode, Event eventRECRN010, Event eventRECRN00XA) {
        return RECRN005C.name().equals(statusCode)
                ? isDifferenceGreaterOrEqualToStockDuration(eventRECRN010.getStatusTimestamp(), eventRECRN00XA.getStatusTimestamp())
                : isDifferenceGreaterRefinementDuration(eventRECRN010.getStatusTimestamp(), eventRECRN00XA.getStatusTimestamp());
    }

    private EventStatusCodeEnum getRECRN00XA(String statusCode) {
        final String status = statusCode.substring(0, statusCode.length() - 1).concat("A");
        return EventStatusCodeEnum.fromKey(status);
    }

    private Mono<Void> prepareFinalEventAndPNRN012toSend(HandlerContext context, Event finalEvent, Event recrn010) {
        OffsetDateTime pnrn012Time = addDurationToInstant(recrn010.getStatusTimestamp(), pnPaperTrackerConfigs.getRefinementDuration()).atOffset(ZoneOffset.UTC);

        return SendEventMapper.createSendEventsFromEventEntity(context.getTrackingId(), finalEvent, StatusCodeEnum.OK, PNRN012.name(), pnrn012Time)
                .doOnNext(context.getEventsToSend()::add)
                .flatMap(se -> buildSendEvent(context, finalEvent, StatusCodeEnum.PROGRESS, finalEvent.getStatusCode(), finalEvent.getStatusTimestamp().atOffset(ZoneOffset.UTC)))
                .doOnNext(context.getEventsToSend()::add)
                .then();
    }

    private Event getEvent(List<Event> events, EventStatusCodeEnum code) {
        return events.stream()
                .filter(e -> code.name().equals(e.getStatusCode()))
                .findFirst()
                .orElseThrow();
    }

    /**
     * Calculates the temporal distance between two {@link Instant}s according to the
     * “truncate‑to‑date” settings.
     *
     * <p><strong>Behaviour</strong></p>
     * <ul>
     *   <li><b>Date‑based mode</b> –&nbsp;If
     *       {@link it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs#isEnableTruncatedDateForRefinementCheck()}
     *       returns {@code true}, each instant is first converted to the civil date in the {@code Europe/Rome}
     *       time‑zone (see {@link #toRomeDate(Instant)}).
     *       The method then returns a {@link Duration} equal to the number of <em>calendar
     *       days</em> between those two dates:
     *       {@code Duration.ofDays(…)}. Sub‑day information and daylight‑saving gaps/overlaps are
     *       deliberately ignored.</li>
     *   <li><b>Time‑based mode</b> –&nbsp;If the flag is {@code false}, the method falls back to
     *       {@link Duration#between( java.time.temporal.Temporal, java.time.temporal.Temporal)}
     *       and preserves the full time‑of‑day component (hours, minutes, seconds, nanoseconds).</li>
     * </ul>
     *
     * @param instant1 the starting instant (inclusive), must not be {@code null}
     * @param instant2 the ending instant   (exclusive), must not be {@code null}
     * @return a {@link Duration} representing either
     *         <ul>
     *           <li>the exact time‑based interval between the two instants, or</li>
     *           <li>a whole‑days interval measured on the local calendar in Europe/Rome,</li>
     *         </ul>
     *         depending on the configuration flag
     *
     * @see #toRomeDate(Instant)
     * @see Duration#between(java.time.temporal.Temporal, java.time.temporal.Temporal)
     * @see it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs#isEnableTruncatedDateForRefinementCheck()
     */
    protected Duration getDurationBetweenDates(Instant instant1, Instant instant2) {
        LocalDate romeDate1 = toRomeDate(instant1);
        LocalDate romeDate2 = toRomeDate(instant2);
        Duration result = pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()
                ? Duration.ofDays(ChronoUnit.DAYS.between(romeDate1, romeDate2))
                : Duration.between(instant1, instant2);
        log.info("Duration between dates i1={} i2={} result={}",instant1, instant2, result);
        return result;
    }

    /**
     * Adds a {@link Duration} to an {@link Instant}, respecting the
     * “truncate‑to‑date” setting.
     *
     * <p>
     * If {@link it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs#isEnableTruncatedDateForRefinementCheck()}
     * is {@code true}, the instant is first converted to its calendar date in {@code Europe/Rome};
     * only the whole‑days part of the duration ({@link Duration#toDays()}) is applied,
     * and the result is returned as the start‑of‑day {@link Instant}.
     * Otherwise, the full duration is added in the usual way ({@link Instant#plus(java.time.temporal.TemporalAmount)}).
     * </p>
     *
     * @param instant  the base moment
     * @param duration the amount to add
     * @return the adjusted {@link Instant}
     */
    protected Instant addDurationToInstant(Instant instant, Duration duration) {
        return pnPaperTrackerConfigs.isEnableTruncatedDateForRefinementCheck()
                ? romeDateToInstant(
                toRomeDate(instant).plusDays(duration.toDays()))
                : instant.plus(duration);
    }

    /**
     * Checks whether the time difference between RECRN010 and RECRN00xA is
     * greater than {@link it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs#getRefinementDuration()}.
     *
     * @param recrn010Timestamp  The {@link Instant} of RECRN010.
     * @param recrn00xATimestamp The {@link Instant} of RECRN00xA (e.g., RECRN003A).
     * @return {@code true} if the difference is > the configured refinement duration; otherwise, {@code false}.
     */
    protected boolean isDifferenceGreaterRefinementDuration (
            Instant recrn010Timestamp,
            Instant recrn00xATimestamp) {
        log.debug("recrn010Timestamp={}, recrn00xATimestamp={}, refinementDuration={}",
                recrn010Timestamp, recrn00xATimestamp, pnPaperTrackerConfigs.getRefinementDuration());
        return getDurationBetweenDates(recrn010Timestamp, recrn00xATimestamp)
                .compareTo(pnPaperTrackerConfigs.getRefinementDuration()) > 0;
    }

    /**
     * Checks whether the time difference between RECRN010 and RECRN005A is
     * greater than or equal to {@link it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs#getCompiutaGiacenzaArDuration()}.
     *
     * @param recrn010Timestamp  The {@link Instant} of RECRN010.
     * @param recrn005ATimestamp The {@link Instant} of RECRN005A.
     * @return {@code true} if the difference is &ge; the compiuta giacenza AR duration; otherwise, {@code false}.
     */
    protected boolean isDifferenceGreaterOrEqualToStockDuration (
            Instant recrn010Timestamp,
            Instant recrn005ATimestamp) {
        log.debug("recrn010Timestamp={}, recrn005ATimestamp={}, refinementDuration={}",
                recrn010Timestamp, recrn005ATimestamp, pnPaperTrackerConfigs.getCompiutaGiacenzaArDuration());
        return getDurationBetweenDates(recrn010Timestamp, recrn005ATimestamp)
                .compareTo(pnPaperTrackerConfigs.getCompiutaGiacenzaArDuration()) >= 0;
    }

    /**
     * Converts the supplied {@link Instant} to a calendar date in the
     * {@code Europe/Rome} time zone.
     *
     * @param instant the moment in time to convert; must not be {@code null}
     * @return the corresponding {@link LocalDate} in Europe/Rome
     */
    private LocalDate toRomeDate(Instant instant) {
        return instant.atZone(ZoneId.of("Europe/Rome"))
                .toLocalDate();
    }

    /**
     * Converts the supplied {@link LocalDate} to an {@link Instant} that marks
     * the start of that day (00:00) in the {@code Europe/Rome} time zone.
     *
     * @param date the calendar day to convert; must not be {@code null}
     * @return the {@link Instant} at the start of the given day in Europe/Rome
     */
    private Instant romeDateToInstant(LocalDate date) {
        ZoneId rome = ZoneId.of("Europe/Rome");
        return date.atStartOfDay(rome)
                .toInstant();
    }
}