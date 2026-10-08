package it.pagopa.pn.papertracker.it;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.it.model.TestEvent;
import it.pagopa.pn.papertracker.it.validator.ErrorValidator;
import it.pagopa.pn.papertracker.it.validator.OutputValidator;
import it.pagopa.pn.papertracker.it.validator.TrackingValidator;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.OcrEventHandler;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import it.pagopa.pn.papertracker.service.NotificationReworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorCategory.OCR_VALIDATION;
import static java.util.stream.Collectors.groupingBy;


@Component
@RequiredArgsConstructor
public class GenericTestCaseHandlerImpl implements GenericTestCaseHandler {

    private final ExternalChannelHandler externalChannelHandler;
    private final PaperTrackingsErrorsDAO paperTrackingsErrorsDAO;
    private final PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDao;
    private final PaperTrackingsDAO paperTrackingsDAO;
    private final OcrEventHandler ocrEventHandler;
    private final NotificationReworkService notificationReworkService;

    @Override
    public String getProductType() {
        return ProductType.UNKNOWN.getValue();
    }

    @Override
    public void beforeInit(ProductTestCase scenario, boolean strictFinalValidation, OcrStatusEnum ocrStatusEnum) {
        String randomIun = UUID.randomUUID().toString();

        replaceIun(scenario, randomIun);

        List<PaperTrackingsErrors> expErrors = scenario.getExpected().getErrors();

        if(scenario.getName().equalsIgnoreCase("OK_GIACENZA_EMPTY_REGISTEREDLETTERCODE_890")
        && OcrStatusEnum.DISABLED.equals(ocrStatusEnum)){
            scenario.getExpected().setErrors(expErrors.stream()
                    .filter(paperTrackingsErrors -> !paperTrackingsErrors.getErrorCategory().equals(OCR_VALIDATION))
                    .toList());
        }

        //se è attiva a la strict validation per lo stock890,
        // in caso di errori di sequence validation vengono convertiti in errori bloccanti e vengono rimossi tutti gli eventi di output con status detail
        // che termina con C (l'evento finale non sale in timeline come progress)
        //poichè la validazione non è andata a buon fine deve essere aggiornato il paperStatus rimuovendo i valdiatedEvent, il registeredLetterCode e
        //la deliveryFailureCause e impostando come finalStatusCode il "RECAG012"
        if (strictFinalValidation) {
            List<PaperTrackings> expTrackings = scenario.getExpected().getTrackings();
            List<PaperTrackerDryRunOutputs> expOutputs = scenario.getExpected().getOutputs();

            if (expErrors.stream().anyMatch(paperTrackingsErrors ->
                    paperTrackingsErrors.getFlowThrow().equals(FlowThrow.SEQUENCE_VALIDATION)) && !scenario.getName().equalsIgnoreCase("OK_TIMESTAMPERROR_890")) {

                replaceOutputsFields(scenario, expOutputs);
                replaceTrackingsFields(scenario, expTrackings);
            }

            replaceErrorsFields(scenario, expErrors);
        }

    }

    private static void replaceErrorsFields(ProductTestCase scenario, List<PaperTrackingsErrors> expErrors) {
        scenario.getExpected().getErrors().stream()
                .filter(paperTrackingsErrors -> paperTrackingsErrors.getFlowThrow().equals(FlowThrow.SEQUENCE_VALIDATION))
                .forEach(paperTrackingsErrors -> paperTrackingsErrors.setType(ErrorType.ERROR));

        if (scenario.getName().equalsIgnoreCase("OK_GIACENZA_MORE_ERROR_890")) {
            scenario.getExpected().setErrors(expErrors.stream()
                    .filter(paperTrackingsErrors -> paperTrackingsErrors.getErrorCategory().equals(ErrorCategory.DATE_ERROR))
                    .toList());
        }
        if (scenario.getName().contains("INVALID_ATTACHMENT")) {
            scenario.getExpected().setErrors(expErrors.stream()
                    .filter(paperTrackingsErrors -> paperTrackingsErrors.getDetails().getMessage().startsWith("Missed required attachments"))
                    .toList());
        }
    }

    private static void replaceOutputsFields(ProductTestCase scenario, List<PaperTrackerDryRunOutputs> expOutputs) {
        scenario.getExpected().setOutputs(expOutputs.stream()
                .filter(paperTrackerDryRunOutputs -> !paperTrackerDryRunOutputs.getStatusDetail().endsWith("C")).toList());
    }

    private static void replaceTrackingsFields(ProductTestCase scenario, List<PaperTrackings> expTrackings) {
        expTrackings.forEach(paperTrackings -> {
            paperTrackings.setBusinessState(BusinessState.KO);
            paperTrackings.getPaperStatus().setValidatedEvents(new ArrayList<>());
            paperTrackings.getPaperStatus().setRegisteredLetterCode(null);
            paperTrackings.getPaperStatus().setDeliveryFailureCause(null);
        });

        if (scenario.getExpected().getTrackings().stream().anyMatch(paperTrackings -> Objects.nonNull(paperTrackings.getPaperStatus().getFinalStatusCode()))) {
            scenario.getExpected().getTrackings().forEach(paperTrackings -> paperTrackings.getPaperStatus().setFinalStatusCode("RECAG012"));
        }
    }

    private static void replaceIun(ProductTestCase scenario, String randomIun) {
        scenario.getInitialTracking().setAttemptId(
                scenario.getInitialTracking().getAttemptId()
                        .replace("{{RANDOM_IUN}}", randomIun)
        );

        scenario.getEvents()
                .stream().filter(testEvent -> Objects.nonNull(testEvent.getAnalogMail()))
                .forEach(testEvent -> testEvent.getAnalogMail().setRequestId(
                testEvent.getAnalogMail().getRequestId().replace("{{RANDOM_IUN}}", randomIun)));

        scenario.getExpected().getTrackings().forEach(out -> {
            out.setTrackingId(out.getTrackingId().replace("{{RANDOM_IUN}}", randomIun));
            out.setAttemptId(out.getAttemptId().replace("{{RANDOM_IUN}}", randomIun));
        });
    }

    @Override
    public void afterInit(ProductTestCase scenario, TrackingCreationRequest request) {

        String requestId = String.join(".", request.getAttemptId(), request.getPcRetry());
        String retryRequestId = requestId.replace("PCRETRY_0", "PCRETRY_1");
        String secondRetryRequestId = requestId.replace("PCRETRY_0", "PCRETRY_2");

        if (!CollectionUtils.isEmpty(scenario.getEvents())) {
            scenario.getEvents()
                    .stream().filter(testEvent -> Objects.nonNull(testEvent.getAnalogMail()))
                    .forEach(event -> {
                String original = event.getAnalogMail().getRequestId();
                if (original.contains("{{REQUEST_ID_RETRY}}")) {
                    event.getAnalogMail().setRequestId(retryRequestId);
                } else if (original.contains("{{REQUEST_ID_RETRY_2}}")) {
                    event.getAnalogMail().setRequestId(secondRetryRequestId);
                } else {
                    event.getAnalogMail().setRequestId(requestId);
                }
            });
        }
        if (Objects.nonNull(scenario.getExpected())) {
            scenario.getExpected().getOutputs().forEach(event -> {
                if (event.getTrackingId().contains("{{REQUEST_ID_RETRY}}")) {
                    event.setTrackingId(retryRequestId);
                } else if (event.getTrackingId().contains("{{REQUEST_ID_RETRY_2}}")) {
                    event.setTrackingId(secondRetryRequestId);
                } else {
                    event.setTrackingId(requestId);
                }
            });
            scenario.getExpected().getErrors().forEach(event -> {
                if (event.getTrackingId().contains("{{REQUEST_ID_RETRY}}")) {
                    event.setTrackingId(retryRequestId);
                } else if (event.getTrackingId().contains("{{REQUEST_ID_RETRY_2}}")) {
                    event.setTrackingId(secondRetryRequestId);
                } else {
                    event.setTrackingId(requestId);
                }
            });
            scenario.getExpected().getTrackings().forEach(paperTrackings -> {
                if (paperTrackings.getTrackingId().contains("{{REQUEST_ID_RETRY}}")) {
                    paperTrackings.setTrackingId(retryRequestId);
                    if (Objects.nonNull(paperTrackings.getNextRequestIdPcretry())) {
                        paperTrackings.setNextRequestIdPcretry(secondRetryRequestId);
                    }
                } else if (paperTrackings.getTrackingId().contains("{{REQUEST_ID_RETRY_2}}")) {
                    paperTrackings.setTrackingId(secondRetryRequestId);
                } else {
                    paperTrackings.setTrackingId(requestId);
                    if (Objects.nonNull(paperTrackings.getNextRequestIdPcretry())) {
                        paperTrackings.setNextRequestIdPcretry(retryRequestId);
                    }
                }
            });
            if (Objects.nonNull(scenario.getExpected().getOcrResultPayload())) {
                scenario.getExpected().getOcrResultPayload().forEach(ocrDataResultPayload ->
                        ocrDataResultPayload.setCommandId(ocrDataResultPayload.getCommandId().replace("{{REQUEST_ID_RETRY}}", retryRequestId)
                        .replace("{{REQUEST_ID_RETRY_2}}", secondRetryRequestId)
                        .replace("{{REQUEST_ID}}", requestId)));
                scenario.getExpected().getOcrDataPayload().forEach(ocrDataResultPayload -> ocrDataResultPayload.setCommandId(ocrDataResultPayload.getCommandId().replace("{{REQUEST_ID_RETRY}}", retryRequestId)
                        .replace("{{REQUEST_ID_RETRY_2}}", secondRetryRequestId)
                        .replace("{{REQUEST_ID}}", requestId)));
            }
        }

        resolvePcRetryResponse(scenario);
    }

    private static void resolvePcRetryResponse(ProductTestCase scenario) {
        if (!CollectionUtils.isEmpty(scenario.getEvents())) {
            scenario.getEvents().stream()
                    .map(TestEvent::getAnalogMail)
                    .filter(Objects::nonNull)
                    .filter(item -> item.getRequestId().endsWith("PCRETRY_1"))
                    .findFirst()
                    .ifPresent(item -> scenario.getFirstPcRetryResponse().setRequestId(item.getRequestId()));

            scenario.getEvents().stream()
                    .map(TestEvent::getAnalogMail)
                    .filter(Objects::nonNull)
                    .filter(item -> item.getRequestId().endsWith("PCRETRY_2"))
                    .findFirst()
                    .ifPresent(item -> scenario.getSecondPcRetryResponse().setRequestId(item.getRequestId()));
        }
    }

    @Override
    public void sendEventsAndOcrResponse(ProductTestCase scenario, OcrStatusEnum ocrStatusEnum) {

        List<TestEvent> events = scenario.getEvents();
        boolean ocrEnabled = !OcrStatusEnum.DISABLED.equals(ocrStatusEnum);
        boolean hasEvents = !CollectionUtils.isEmpty(events);

        if (hasEvents) {
            for (TestEvent event : events) {
                boolean isOcrMessage = "SEND_OCR_RESPONSE".equalsIgnoreCase(event.getMessageId());
                boolean runRework = "RUN_REWORK".equalsIgnoreCase(event.getMessageId());
                if (runRework) {
                    notificationReworkService.updatePaperTrackingsStatusForRework(event.getAnalogMail().getRequestId(), "REWORK_0").block();
                } else if (!isOcrMessage) {
                    SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
                    singleStatusUpdate.setAnalogMail(event.getAnalogMail());
                    externalChannelHandler.handleExternalChannelMessage(
                            singleStatusUpdate,
                            true,
                            null,
                            event.getMessageId(),
                            null
                    );
                } else if (ocrEnabled) {
                    ocrEventHandler.handleOcrMessage(
                            scenario.getExpected()
                                    .getOcrResultPayload()
                                    .get(event.getOcrResponseIdx() - 1)
                    );
                }
            }
        }

        boolean noOcrEventInScenario = hasEvents && events.stream().noneMatch(e -> "SEND_OCR_RESPONSE".equalsIgnoreCase(e.getMessageId()));
        boolean hasOcrPayload = Objects.nonNull(scenario.getExpected().getOcrResultPayload());
        if (noOcrEventInScenario && hasOcrPayload && ocrEnabled) {
            var firstPayload = scenario.getExpected().getOcrResultPayload().getFirst();
            ocrEventHandler.handleOcrMessage(firstPayload);
            boolean isDuplicateScenario = scenario.getName().equalsIgnoreCase("OK_DUPLICATE_OCR_RESPONSE_AR") ||
                            scenario.getName().equalsIgnoreCase("OK_DUPLICATE_OCR_RESPONSE_890");

            if (isDuplicateScenario) {
                ocrEventHandler.handleOcrMessage(firstPayload);
            }
        }
    }

    @Override
    public void afterSendEvents(ProductTestCase scenario, OcrStatusEnum ocrStatusEnum, boolean strictValidation) {
        if (!CollectionUtils.isEmpty(scenario.getEvents())) {
            Set<String> requestIds = scenario.getEvents().stream()
                    .filter(testEvent -> Objects.nonNull(testEvent.getAnalogMail()))
                    .collect(groupingBy(event -> event.getAnalogMail().getRequestId()))
                    .keySet();

            List<PaperTrackingsErrors> errors = new ArrayList<>();
            List<PaperTrackerDryRunOutputs> outputs = new ArrayList<>();
            List<PaperTrackings> trackings = paperTrackingsDAO.retrieveAllByTrackingIds(requestIds.stream().toList()).collectList().block();

            requestIds.forEach(requestId -> errors.addAll(Objects.requireNonNull(paperTrackingsErrorsDAO.retrieveErrors(requestId).collectList().block())));
            ErrorValidator.verifyErrors(scenario, errors);

            requestIds.forEach(requestId -> outputs.addAll(Objects.requireNonNull(paperTrackerDryRunOutputsDao.retrieveOutputEvents(requestId).collectList().block())));
            OutputValidator.verifyOutputs(scenario, ocrStatusEnum, outputs);

            TrackingValidator.verifyTrackingEntities(scenario, trackings, ocrStatusEnum, strictValidation);
        }
    }

    public static void getPcRetryResponse(ProductTestCase scenario) {
        PcRetryResponse response = new PcRetryResponse();
        response.setRetryFound(true);
        response.setPcRetry("PCRETRY_1");

        PcRetryResponse response2 = new PcRetryResponse();
        response2.setRetryFound(true);
        response2.setPcRetry("PCRETRY_2");

        if (!CollectionUtils.isEmpty(scenario.getEvents())) {

            scenario.getEvents().stream()
                    .map(TestEvent::getAnalogMail)
                    .filter(Objects::nonNull)
                    .filter(item -> item.getRequestId().endsWith("PCRETRY_1"))
                    .findFirst()
                    .ifPresent(item -> {
                        response.setRequestId(item.getRequestId());
                    });

            scenario.getEvents().stream()
                    .map(TestEvent::getAnalogMail)
                    .filter(Objects::nonNull)
                    .filter(item -> item.getRequestId().endsWith("PCRETRY_2"))
                    .findFirst()
                    .ifPresent(item -> {
                        response2.setRequestId(item.getRequestId());
                    });
        }

        scenario.setFirstPcRetryResponse(response);
        scenario.setSecondPcRetryResponse(response2);
    }
}
