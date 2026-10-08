package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.DeliveryFailureCauseEnum;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfig;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfiguration;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorCause.INVALID_VALUES;
import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorCause.VALUES_NOT_MATCHING;
import static it.pagopa.pn.papertracker.model.DocumentTypeEnum.ARCAD;
import static it.pagopa.pn.papertracker.model.DocumentTypeEnum.CAD;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG012;
import static it.pagopa.pn.papertracker.model.PredictedRefinementType.PRE10;
import static it.pagopa.pn.papertracker.utils.TrackerUtility.createAffectedEventsMap;

@RequiredArgsConstructor
@Slf4j
public abstract class GenericSequenceValidator implements HandlerStep {

    private final PaperTrackingsDAO paperTrackingsDAO;
    private final PaperTrackerErrorService paperTrackerErrorService;

    /**
     * Step di validazione della sequenza degli eventi di una raccomandata.
     * La validazione include controlli sulla presenza di eventi necessari, coerenza dei timestamp
     * di business, validità dei documenti allegati, coerenza dei codici di lettera raccomandata e
     * correttezza della causa di mancata consegna.
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        SequenceConfig sequenceConfig = SequenceConfiguration.getConfig(context.getPaperProgressStatusEvent().getStatusCode());

        log.debug("Executing GenericSequenceValidator with sequence config: {}", sequenceConfig);
        return Mono.just(context.getPaperTrackings())
                .flatMap(paperTrackings -> validateSequence(paperTrackings, context, sequenceConfig, true))
                .doOnNext(TrackerUtility::checkValidationConfig)//aggiunto per retrocompatibilità
                .doOnNext(context::setPaperTrackings)
                .then();
    }

    /**
     * Valida la sequenza degli eventi di una raccomandata, applicando una serie di controlli di business.
     *
     * @param paperTrackings oggetto contenente gli eventi da validare
     * @return Mono<Void> che completa se la validazione ha successo, altrimenti emette un Mono.error
     */
    public Mono<PaperTrackings> validateSequence(PaperTrackings paperTrackings, HandlerContext context, SequenceConfig sequenceConfig, Boolean strictFinalEventValidation) {
        PaperTrackings paperTrackingsToUpdate = new PaperTrackings();
        paperTrackingsToUpdate.setPaperStatus(new PaperStatus());
        log.info("Starting validation for sequence for paper tracking : {}", paperTrackings);
        return extractSequenceFromEvents(paperTrackings.getEvents(), sequenceConfig.sequenceStatusCodes(), context.getReworkId())
                .filter(events -> !CollectionUtils.isEmpty(events))
                .switchIfEmpty(Mono.defer(() -> getErrorOrSaveWarning("Invalid lastEvent for sequence validation", context, paperTrackings, ErrorCategory.LAST_EVENT_EXTRACTION_ERROR,null,null,  strictFinalEventValidation, Collections.emptyList())))
                .flatMap(this::getOnlyLatestEvents)
                .flatMap(events -> validatePresenceOfStatusCodes(events, paperTrackings, context, sequenceConfig.requiredStatusCodes(),strictFinalEventValidation))
                .flatMap(events -> validateBusinessTimestamps(events, paperTrackings, context, sequenceConfig,strictFinalEventValidation, paperTrackingsToUpdate))
                .flatMap(events -> validateAttachments(events, paperTrackings, context, sequenceConfig.validAttachments(), sequenceConfig.requiredAttachments(),strictFinalEventValidation))
                .flatMap(events -> validateRegisteredLetterCode(events, paperTrackings, paperTrackingsToUpdate, context,strictFinalEventValidation))
                .flatMap(events -> validateDeliveryFailureCause(events, paperTrackings, context,strictFinalEventValidation))
                .flatMap(events -> enrichPaperTrackingToUpdateWithAddressAndFailureCause(events, paperTrackingsToUpdate, TrackerUtility.extractEventFromContext(context).getStatusCode()))
                .flatMap(events -> paperTrackingsDAO.updateItem(paperTrackings.getTrackingId(), enrichWithSequenceValidationTimestamp(events, paperTrackingsToUpdate)));
    }

    private Mono<List<Event>> enrichPaperTrackingToUpdateWithAddressAndFailureCause(List<Event> events, PaperTrackings paperTrackingsToUpdate, String statusCode) {
        return Mono.justOrEmpty(events.stream()
                        .filter(e -> e.getStatusCode().equalsIgnoreCase(preCloseMetaStatusCode(statusCode)))
                        .findFirst())
                .doOnNext(preCloseEvent -> {
                    if (StringUtils.hasText(preCloseEvent.getAnonymizedDiscoveredAddressId())) {
                        paperTrackingsToUpdate.getPaperStatus().setAnonymizedDiscoveredAddress(preCloseEvent.getAnonymizedDiscoveredAddressId());
                    }
                    if (StringUtils.hasText(preCloseEvent.getDeliveryFailureCause())) {
                        paperTrackingsToUpdate.getPaperStatus().setDeliveryFailureCause(preCloseEvent.getDeliveryFailureCause());
                    }
                })
                .thenReturn(events);
    }

    private String preCloseMetaStatusCode(String finalStatusCode) {
        String lastLetter = finalStatusCode.substring(finalStatusCode.length() - 1);
        if (lastLetter.equals("C")) {
            return finalStatusCode.substring(0, finalStatusCode.length() - 1) + "A";
        } else if (lastLetter.equals("F")) {
            return finalStatusCode.substring(0, finalStatusCode.length() - 1) + "D";
        }
        return finalStatusCode;
    }

    /**
     * Valida la presenza dei documenti necessari presenti nella lista di eventi in input. La lista dei documenti necessari
     * viene ottenuta dalle configurazione e nel caso in cui si abbiano documenti in più non richiesti non mandiamo in errore
     * ma continuiamo il flusso.
     * <p>
     * Esempio lista OK:
     * RECRN002D
     * RECRN002E[AR, INDAGINE]
     * RECRN002E[AR]
     * RECRN002F
     * Esempio lista KO:
     * RECRN002D
     * RECRN002E[PLICO]
     * RECRN002E[INDAGINE]
     * RECRN002F
     *
     * @param events         lista di eventi da cui estrarre la sequenza
     * @param paperTrackings entità di paper tracking
     * @return Mono contenente la lista di eventi dati in input, altrimenti se la validazione non è andata a buona fine Mono.error()
     */
    private Mono<List<Event>> validateAttachments(List<Event> events, PaperTrackings paperTrackings, HandlerContext context,
                                                  Map<String, Set<String>> validAttachments, Set<String> requiredAttachments, Boolean strictFinalEventValidation) {
        log.info("Beginning attachment validation for events: {}", events);

        var eventsWithAttachments = events.stream()
                .filter(event -> !CollectionUtils.isEmpty(event.getAttachments()))
                .toList();

        var allDocs = eventsWithAttachments.stream()
                .flatMap(event -> event.getAttachments().stream().map(Attachment::getDocumentType))
                .collect(Collectors.toSet());

        return verifyRequiredAttachments(events, paperTrackings, context, requiredAttachments, allDocs, strictFinalEventValidation)
                .flatMap(unused -> verifyValidAttachments(events, paperTrackings, context, validAttachments, eventsWithAttachments, strictFinalEventValidation))
                .doOnNext(unused -> log.info("Attachments validation completed successfully"));
    }

    private Mono<List<Event>> verifyRequiredAttachments(List<Event> events, PaperTrackings paperTrackings, HandlerContext context, Set<String> requiredAttachments, Set<String> allDocs, Boolean strictFinalEventValidation) {
        var missingDocs = new HashSet<>(requiredAttachments);
        missingDocs.removeAll(allDocs);

        if (allDocs.contains(CAD.getValue()) || allDocs.contains(ARCAD.getValue())) {
            missingDocs.remove(CAD.getValue());
            missingDocs.remove(ARCAD.getValue());
        }

        if (missingDocs.isEmpty()) {
            return Mono.just(events);
        }
        Map<String, Object> additionalDetails = Map.of("missingAttachments", missingDocs.stream().toList());
        return getErrorOrSaveWarning
                ("Missed required attachments for the sequence validation: " + missingDocs, context, paperTrackings, ErrorCategory.ATTACHMENTS_ERROR, VALUES_NOT_MATCHING, additionalDetails, strictFinalEventValidation, events);    }

    private Mono<List<Event>> verifyValidAttachments(List<Event> events, PaperTrackings paperTrackings, HandlerContext context, Map<String, Set<String>> validAttachments, List<Event> eventsWithAttachments, Boolean strictFinalEventValidation) {
        for (Event e : eventsWithAttachments) {
            var allowedDocs = validAttachments.get(e.getStatusCode());

            var eventDocs = e.getAttachments().stream()
                    .map(Attachment::getDocumentType)
                    .collect(Collectors.toSet());

            // Se l'evento contiene documenti non ammessi
            var invalidDocs = eventDocs.stream()
                    .filter(doc -> CollectionUtils.isEmpty(allowedDocs) || !allowedDocs.contains(doc))
                    .collect(Collectors.toSet());

            if (!CollectionUtils.isEmpty(invalidDocs)) {
                Map<String, Object> additionalDetails = Map.of("invalidAttachments",  invalidDocs.stream().toList());

                return getErrorOrSaveWarning("Event " + e.getStatusCode() + " contains invalid attachments: " + invalidDocs, context, paperTrackings, ErrorCategory.ATTACHMENTS_ERROR, INVALID_VALUES, additionalDetails, strictFinalEventValidation, events);
            }
        }
        return Mono.just(events);
    }

    /**
     * Filtra la lista di eventi per ottenere solo gli ultimi eventi rilevanti.
     * Nel caso di eventi contententi documenti, il codice prenderà gli eventi che hanno un documento non presente nella
     * mappa dei documenti già presi
     * <p>
     * Ad esempio per la seguente lista prenderemo i seguenti eventi
     * -> RECRN002D
     * RECRN002E[INDAGINE]
     * -> RECRN002E[PLICO]
     * -> RECRN002E[INDAGINE]
     * RECRN002E[AR]
     * -> RECRN002E[AR]
     * -> RECRN002F
     * <p>
     * similmente nel caso di evento con più documenti, prende l'evento se uno tra i documenti dell'evento
     * non è nella mappa ad esempio:
     * -> RECRN002D
     * RECRN002E[INDAGINE]
     * -> RECRN002E[PLICO]
     * RECRN002E[INDAGINE]
     * -> RECRN002E[AR, INDAGINE]
     * -> RECRN002E[AR]
     * -> RECRN002F
     *
     * @param events lista di eventi da filtrare
     * @return Mono di eventi contenenti gli elementi filtrati dal metodo
     */
    public Mono<List<Event>> getOnlyLatestEvents(List<Event> events) {
        log.info("Beginning extraction of latest events from : {}", events);
        HashMap<String, Set<String>> uniqueCodes = new HashMap<>();
        List<Event> mutableEvents = new ArrayList<>(events);

        //Ordino la lista in base al timestamp e poi la inverto per avere al primo posto l'evento con requestTimestamp più recente
        mutableEvents.sort(Comparator.comparing(Event::getRequestTimestamp).reversed());

        return Mono.just(mutableEvents.stream().filter(
                event -> {
                    Set<String> documents = uniqueCodes.get(event.getStatusCode());
                    boolean allDocumentsInEventAreAlreadyPresentInMap =
                            documents != null &&
                                    Optional.ofNullable(event.getAttachments()).orElse(new ArrayList<>())
                                            .stream()
                                            .allMatch(document -> documents.contains(document.getDocumentType()));
                    if (allDocumentsInEventAreAlreadyPresentInMap) {
                        return false;
                    } else {
                        if (documents == null) {
                            uniqueCodes.put(event.getStatusCode(), new HashSet<>());
                        }
                        if (!CollectionUtils.isEmpty(event.getAttachments())) {
                            List<Attachment> documentsToAdd = event.getAttachments().stream()
                                    .filter(document -> !uniqueCodes.get(event.getStatusCode())
                                            .contains(document.getDocumentType()))
                                    .toList();
                            if (!CollectionUtils.isEmpty(documentsToAdd)) {
                                Set<String> eventDocuments = uniqueCodes.get(event.getStatusCode());
                                eventDocuments.addAll(documentsToAdd.stream().map(Attachment::getDocumentType).toList());
                                uniqueCodes.put(event.getStatusCode(), eventDocuments);
                            }
                        }
                        return true;
                    }
                }
        ).toList());
    }

    /**
     * Valida la presenza e la correttezza della deliveryFailureCause negli eventi.<br>
     * Il controllo viene effettuato sulla base di quanto censito nell'enum {@link it.pagopa.pn.papertracker.model.EventStatusCodeEnum}:<br>
     * - Se per lo status code dell'evento, come deliveryFailureCauseList, risulta censita CHECK_IF_REQUIRED,
     * allora la validazione viene saltata<br>
     * - Se per lo status code dell'evento, come deliveryFailureCauseList, risulta censita una lista vuota o una lista con valori specifici,
     * allora viene controllato che la deliveryFailureCause dell'evento sia assente (nel primo caso) o presente e valida (nel secondo caso)<br>
     *
     * @param events         lista di eventi da validare
     * @param paperTrackings oggetto principale della richiesta
     * @return Mono contenente la lista di eventi dati in input, altrimenti se la validazione non è andata a buona fine Mono.error()
     */
    protected Mono<List<Event>> validateDeliveryFailureCause(List<Event> events, PaperTrackings paperTrackings, HandlerContext context, Boolean strictFinalEventValidation) {
        log.info("Beginning validation for delivery failure cause for events : {}", events);
        return Flux.fromIterable(events)
                .flatMap(event -> {
                    EventStatusCodeEnum statusCodeEnum = EventStatusCodeEnum.fromKey(event.getStatusCode());
                    List<DeliveryFailureCauseEnum> allowedCauses = statusCodeEnum.getDeliveryFailureCauseList();
                    return checkPresenceOfCause(event, context, paperTrackings, allowedCauses, strictFinalEventValidation);
                })
                .flatMap(event -> {
                    EventStatusCodeEnum statusCodeEnum = EventStatusCodeEnum.fromKey(event.getStatusCode());
                    List<DeliveryFailureCauseEnum> allowedCauses = statusCodeEnum.getDeliveryFailureCauseList();
                    return checkIfIsValidCause(context, paperTrackings, strictFinalEventValidation, allowedCauses, event)
                            .flatMap(unused -> checkIfStrictValidation(context, paperTrackings, strictFinalEventValidation, allowedCauses, event));
                })
                .filter(event -> StringUtils.hasText(event.getDeliveryFailureCause()))
                .collectList()
                .filter(filteredEvents -> !CollectionUtils.isEmpty(filteredEvents))
                .flatMap(filteredEvents -> allDeliveryFailureCauseAreEquals(context, paperTrackings, strictFinalEventValidation, filteredEvents))
                .thenReturn(events);
    }

    protected Mono<Event> checkIfIsValidCause(HandlerContext context, PaperTrackings paperTrackings, boolean strictFinalEventValidation, List<DeliveryFailureCauseEnum> allowedCauses, Event event) {
        String deliveryFailureCause = event.getDeliveryFailureCause();
        if (allowedCauses.contains(DeliveryFailureCauseEnum.CHECK_IF_REQUIRED) || !StringUtils.hasText(deliveryFailureCause)) {
            return Mono.just(event);
        }

        DeliveryFailureCauseEnum causeEnum = DeliveryFailureCauseEnum.fromValue(deliveryFailureCause);
        boolean isValidCause = allowedCauses.contains(causeEnum);
        if (!isValidCause) {
            Map<String, Object> additionalDetails = createAffectedEventsMap(true, List.of(event));
            return getErrorOrSaveWarning(
                    "Invalid deliveryFailureCause: " + deliveryFailureCause,
                    context,
                    paperTrackings,
                    ErrorCategory.DELIVERY_FAILURE_CAUSE_ERROR,
                    VALUES_NOT_MATCHING,
                    additionalDetails,
                    strictFinalEventValidation,
                    event
            ).then(Mono.empty());
        }

        return Mono.just(event);
    }

    protected Mono<Event> checkPresenceOfCause(Event event, HandlerContext context, PaperTrackings paperTrackings, List<DeliveryFailureCauseEnum> allowedCauses, boolean strictFinalEventValidation) {
        String cause = event.getDeliveryFailureCause();
        boolean hasCause = StringUtils.hasText(cause);
        boolean hasAllowed = !CollectionUtils.isEmpty(allowedCauses);
        Map<String, Object> additionalDetails = createAffectedEventsMap(true, List.of(event));
        if (!hasAllowed && hasCause) {
            return getErrorOrSaveWarning(
                    "Invalid deliveryFailureCause: " + cause,
                    context,
                    paperTrackings,
                    ErrorCategory.DELIVERY_FAILURE_CAUSE_ERROR,
                    VALUES_NOT_MATCHING,
                    additionalDetails,
                    strictFinalEventValidation,
                    event
            ).then(Mono.empty());
        }

        if (hasAllowed && !hasCause && !allowedCauses.contains(DeliveryFailureCauseEnum.CHECK_IF_REQUIRED)) {
            return getErrorOrSaveWarning(
                    "Missing deliveryFailureCause",
                    context,
                    paperTrackings,
                    ErrorCategory.DELIVERY_FAILURE_CAUSE_ERROR,
                    INVALID_VALUES,
                    additionalDetails,
                    strictFinalEventValidation,
                    event
            ).then(Mono.empty());
        }
        return Mono.just(event);
    }

    protected Mono<Event> checkIfStrictValidation(HandlerContext context, PaperTrackings paperTrackings, boolean strictFinalEventValidation, List<DeliveryFailureCauseEnum> allowedCauses, Event event) {
        boolean isStrictValidation = Boolean.TRUE.equals(paperTrackings.getValidationConfig().getStrictDeliveryFailureCause());
        if (isStrictValidation && allowedCauses.contains(DeliveryFailureCauseEnum.CHECK_IF_REQUIRED) && !StringUtils.hasText(event.getDeliveryFailureCause())) {
            Map<String, Object> additionalDetails = createAffectedEventsMap(true, List.of(event));
            return getErrorOrSaveWarning(
                    "Missing deliveryFailureCause",
                    context,
                    paperTrackings,
                    ErrorCategory.DELIVERY_FAILURE_CAUSE_ERROR,
                    INVALID_VALUES,
                    additionalDetails,
                    strictFinalEventValidation,
                    event
            ).then(Mono.empty());
        }
        return Mono.just(event);
    }

    protected Mono<List<Event>> allDeliveryFailureCauseAreEquals(HandlerContext context, PaperTrackings paperTrackings, boolean strictFinalEventValidation, List<Event> events) {
        List<String> deliveryFailureCauses = events.stream()
                .map(Event::getDeliveryFailureCause)
                .map(String::toUpperCase)
                .toList();

        boolean areEquals = deliveryFailureCauses.stream()
                .map(DeliveryFailureCauseEnum::fromValue)
                .distinct()
                .count() <= 1;

        if (!areEquals) {
            Map<String, Object> additionalDetails = createAffectedEventsMap(true, events);
            return getErrorOrSaveWarning(
                    "Invalid deliveryFailureCause on events: " + deliveryFailureCauses,
                    context,
                    paperTrackings,
                    ErrorCategory.DELIVERY_FAILURE_CAUSE_ERROR,
                    VALUES_NOT_MATCHING,
                    additionalDetails,
                    strictFinalEventValidation,
                    events
            );
        }
        paperTrackings.getPaperStatus().setDeliveryFailureCause(deliveryFailureCauses.getFirst());
        return Mono.just(events);
    }

    /**
     * Verifica che tutti i registered letter code degli eventi siano uguali.
     *
     * @param paperTrackings oggetto principale della richiesta
     * @param events         lista di eventi da validare
     * @return Mono contenente la lista di eventi dati in input, altrimenti se la validazione non è andata a buona fine Mono.error()
     */
    private Mono<List<Event>> validateRegisteredLetterCode(List<Event> events, PaperTrackings paperTrackings, PaperTrackings paperTrackingsToUpdate, HandlerContext context, Boolean strictFinalEventValidation) {
        log.info("Beginning validation for registered letter codes for events : {}", events);

        // escludo RECAG012 dalla validazione in quanto potrebbe avere registeredLetterCode diverso
        List<Event> filteredEvents = events.stream().filter(event -> !RECAG012.name().equalsIgnoreCase(event.getStatusCode())).toList();

        String firstRegisteredLetterCode = filteredEvents.getFirst().getRegisteredLetterCode();

        if (filteredEvents.stream().anyMatch(event -> !StringUtils.hasText(event.getRegisteredLetterCode()))) {
            Map<String, Object> additionalDetails = createAffectedEventsMap(false, events.stream().filter(event -> !StringUtils.hasText(event.getRegisteredLetterCode())).toList());
            return getErrorOrSaveWarning("Registered letter code is null or empty in one or more events",
                    context, paperTrackings, ErrorCategory.REGISTERED_LETTER_CODE_ERROR, INVALID_VALUES, additionalDetails, strictFinalEventValidation, events);
        }

        if (filteredEvents.stream().anyMatch(event -> !event.getRegisteredLetterCode().equals(firstRegisteredLetterCode))) {
            Map<String, Object> additionalDetails = createAffectedEventsMap(false, events);
            return getErrorOrSaveWarning("Registered letter codes do not match in sequence: "
                            + filteredEvents.stream().map(Event::getRegisteredLetterCode).toList(),
                    context, paperTrackings, ErrorCategory.REGISTERED_LETTER_CODE_ERROR, VALUES_NOT_MATCHING, additionalDetails, strictFinalEventValidation, events);
        }

        paperTrackingsToUpdate.getPaperStatus().setRegisteredLetterCode(firstRegisteredLetterCode);
        return Mono.just(events);
    }

    /**
     * Estrae la sequenza di eventi rilevanti (tripletta ABC/ DEF) dalla lista fornita.
     *
     * @param events lista di eventi da cui estrarre la sequenza
     * @return Mono contenente la lista filtrata di eventi
     */
    private Mono<List<Event>> extractSequenceFromEvents(List<Event> events, Set<String> requiredStatusCodes, String notificationReworkId) {
        log.info("Beginning extraction of relevant events from : {}", events);
        return Mono.just(events.stream()
                    .filter(event -> !StringUtils.hasText(notificationReworkId) || notificationReworkId.equalsIgnoreCase(event.getNotificationReworkId()))
                    .filter(e -> requiredStatusCodes.contains(e.getStatusCode()))
                    .toList());
    }

    /**
     * Valida la presenza di tutti gli eventi necessari per la sequenza data dall'evento finale.
     *
     * @param events         lista di eventi da validare
     * @param paperTrackings entità di paper tracking
     * @return Mono contenente la lista di eventi dati in input, altrimenti se la validazione non è andata a buona fine Mono.error()
     */
    private Mono<List<Event>> validatePresenceOfStatusCodes(List<Event> events, PaperTrackings paperTrackings, HandlerContext context, Set<String> requiredStatusCodes, Boolean strictFinalEventValidation) {
        log.info("Beginning validation for required status codes for events : {}", events);

        Set<String> eventStatusCodes = events.stream().map(Event::getStatusCode).collect(Collectors.toSet());
        Set<String> missingStatusCodes = new HashSet<>(requiredStatusCodes);
        missingStatusCodes.removeAll(eventStatusCodes);
        if (!missingStatusCodes.isEmpty()) {

            Map<String, Object> additionalDetails = Map.of(
                "missingStatusCodes", String.join(",",missingStatusCodes)
            );
            return getErrorOrSaveWarning(
                    "Necessary status code not found in events: " + missingStatusCodes,
                    context,
                    paperTrackings,
                    ErrorCategory.INCONSISTENT_STATE,
                    ErrorCause.VALUES_NOT_FOUND,
                    additionalDetails,
                    strictFinalEventValidation,
                    events
            );
        }
        return Mono.just(events);
    }

    /**
     * Verifica che i timestamp di business degli eventi siano coerenti tra loro. Questa validazione raggruppa gli eventi
     * in base ad un groupId specificato nella configurazione della sequence. Eventi con groupId uguali verranno comparati
     * tra di loro, eventi con groupId null non verranno validati.
     *
     * @param events         lista di eventi da validare
     * @param paperTrackings entità di tracking
     * @return Mono contenente la lista di eventi dati in input, altrimenti se la validazione non è andata a buona fine Mono.error()
     */
    private Mono<List<Event>> validateBusinessTimestamps(List<Event> events, PaperTrackings paperTrackings, HandlerContext context, SequenceConfig sequenceConfig, Boolean strictFinalEventValidation, PaperTrackings paperTrackingsToUpdate) {
        log.info("Beginning validation for business timestamps for events : {}", events);
        Set<String> finalGroup = sequenceConfig.dateValidationGroupForFinalEvents();
        Set<String> stockGroup = sequenceConfig.dateValidationGroupForStockEvents();

        if(events.stream().anyMatch(event -> Objects.isNull(event.getStatusTimestamp()))){
            Map<String, Object> additionalDetails = createAffectedEventsMap(false, events.stream()
                    .filter(event -> Objects.isNull(event.getStatusTimestamp()))
                    .toList());
            return getErrorOrSaveWarning("Invalid business timestamps", context, paperTrackings, ErrorCategory.DATE_ERROR, INVALID_VALUES, additionalDetails,  strictFinalEventValidation, events);
        }

        Instant validFinal = allStatusTimestampAreEquals(events, finalGroup);
        boolean validStock = allStockStatusTimestampAreEquals(events, stockGroup);

        if (Objects.nonNull(validFinal) && validStock && checkPredictedRefinementTypeIfStock890(paperTrackings, validFinal)) {
            paperTrackingsToUpdate.getPaperStatus().setValidatedSequenceTimestamp(validFinal);
            return Mono.just(events);
        }

        Map<String, Object> additionalDetails = createAffectedEventsMap(false, events.stream().filter(event -> finalGroup.contains(event.getStatusCode())
                || stockGroup.contains(event.getStatusCode())).toList());
        return getErrorOrSaveWarning("Invalid business timestamps", context, paperTrackings, ErrorCategory.DATE_ERROR, VALUES_NOT_MATCHING, additionalDetails, strictFinalEventValidation, events);
    }

    private boolean checkPredictedRefinementTypeIfStock890(PaperTrackings paperTrackings, Instant validFinal) {
        if (Objects.nonNull(paperTrackings.getPaperStatus()) && StringUtils.hasText(paperTrackings.getPaperStatus().getPredictedRefinementType())
                && paperTrackings.getPaperStatus().getPredictedRefinementType().equalsIgnoreCase(PRE10.name())) {
            Optional<Event> RECAG012Event = TrackerUtility.findRECAG012Event(paperTrackings);
            return RECAG012Event.map(event -> event.getStatusTimestamp().equals(validFinal)).orElse(true);
        }
        return true;
    }

    private Instant allStatusTimestampAreEquals(List<Event> events, Set<String> group) {
        List<Instant> timestamps = events.stream()
                .filter(e -> group.contains(e.getStatusCode()))
                .map(Event::getStatusTimestamp)
                .toList();
        if (timestamps.size() <= 1 || timestamps.stream().allMatch(t -> t.equals(timestamps.getFirst()))) {
            return timestamps.getFirst();
        }
        return null;
    }

    private boolean allStockStatusTimestampAreEquals(List<Event> events, Set<String> group) {
        List<Instant> timestamps = events.stream()
                .filter(e -> group.contains(e.getStatusCode()))
                .map(Event::getStatusTimestamp)
                .toList();
        return timestamps.size() <= 1 || timestamps.stream().allMatch(t -> t.equals(timestamps.getFirst()));
    }

    protected <T> Mono<T> getErrorOrSaveWarning(String message, HandlerContext context, PaperTrackings paperTrackings, ErrorCategory errorCategory, ErrorCause errorCause, Map<String, Object> additionalDetails, Boolean strictFinalEventValidation, T returnValue) {
        log.info("getErrorOrSaveWarning for trackingId {}: {} | strictFinalEventValidation: {}", context.getTrackingId(), message, strictFinalEventValidation);

        var error = PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                paperTrackings,
                TrackerUtility.getStatusCodeFromEventId(paperTrackings, context.getEventId()),
                errorCategory,
                errorCause,
                message,
                additionalDetails,
                FlowThrow.SEQUENCE_VALIDATION,
                strictFinalEventValidation ? ErrorType.ERROR : ErrorType.WARNING,
                context.getEventId()
        );

        return strictFinalEventValidation
                ? Mono.error(new PnPaperTrackerValidationException(message, error))
                : paperTrackerErrorService.insertPaperTrackingsError(error).thenReturn(returnValue);
    }

    private PaperTrackings enrichWithSequenceValidationTimestamp(List<Event> events, PaperTrackings paperTrackingsToUpdate) {
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setSequencesValidationTimestamp(Instant.now());
        paperTrackingsToUpdate.setValidationFlow(validationFlow);
        paperTrackingsToUpdate.getPaperStatus().setValidatedEvents(events.stream().map(Event::getId).toList());
        return paperTrackingsToUpdate;
    }

}
