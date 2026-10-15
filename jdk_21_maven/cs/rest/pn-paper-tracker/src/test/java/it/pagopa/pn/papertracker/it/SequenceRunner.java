package it.pagopa.pn.papertracker.it;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import it.pagopa.pn.papertracker.service.PaperTrackerTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SequenceRunner {

    private final List<GenericTestCaseHandler> handlers;
    private final PaperTrackerTrackingService paperTrackerTrackingService;

    public void run(ProductTestCase scenario, OcrStatusEnum ocrStatusEnum, boolean strictFinalValidation) throws InterruptedException {
        GenericTestCaseHandler handler = handlers.stream()
                .filter(h -> h.getProductType()
                        .equalsIgnoreCase(scenario.getProductType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No handler for product " + scenario.getProductType())
                );

        handler.beforeInit(scenario, strictFinalValidation, ocrStatusEnum);
        initPaperTrackings(scenario);
        handler.afterInit(scenario, scenario.getInitialTracking());
        handler.sendEventsAndOcrResponse(scenario, ocrStatusEnum);
        handler.afterSendEvents(scenario, ocrStatusEnum, strictFinalValidation);
    }

    private void initPaperTrackings(ProductTestCase scenario) {
        String xOriginClientId = "clientId";
        paperTrackerTrackingService.insertPaperTrackings(scenario.getInitialTracking(), xOriginClientId).block();
        if(!CollectionUtils.isEmpty(scenario.getEvents())) {

            if (scenario.getEvents().stream()
                    .filter(testEvent -> Objects.nonNull(testEvent.getAnalogMail()))
                    .anyMatch(singleStatusUpdate -> singleStatusUpdate.getAnalogMail()
                            .getRequestId().equalsIgnoreCase("{{REQUEST_ID_RETRY}}"))) {
                paperTrackerTrackingService.insertPaperTrackings(getTrackingCreationRequest(scenario, "PCRETRY_1"), xOriginClientId).block();
            }

            if (scenario.getEvents().stream()
                    .filter(testEvent -> Objects.nonNull(testEvent.getAnalogMail()))
                    .anyMatch(singleStatusUpdate -> singleStatusUpdate.getAnalogMail()
                            .getRequestId().equalsIgnoreCase("{{REQUEST_ID_RETRY_2}}"))) {
                paperTrackerTrackingService.insertPaperTrackings(getTrackingCreationRequest(scenario, "PCRETRY_2"), xOriginClientId).block();
            }
        }
    }

    private static TrackingCreationRequest getTrackingCreationRequest(ProductTestCase scenario, String pcRetry) {
        TrackingCreationRequest retryTrackingCreationRequest = new TrackingCreationRequest();
        retryTrackingCreationRequest.setAttemptId(scenario.getInitialTracking().getAttemptId());
        retryTrackingCreationRequest.setProductType(scenario.getInitialTracking().getProductType());
        retryTrackingCreationRequest.setUnifiedDeliveryDriver(scenario.getInitialTracking().getUnifiedDeliveryDriver());
        retryTrackingCreationRequest.setPcRetry(pcRetry);
        return retryTrackingCreationRequest;
    }
}
