package it.pagopa.pn.papertracker.it.validator;

import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState.DONE;
import static org.junit.jupiter.api.Assertions.*;

public class TrackingValidator {

    public static void verifyTrackingEntities(ProductTestCase scenario, List<PaperTrackings> actualTrackings, OcrStatusEnum ocrStatusEnum, boolean strictFinalValidationStock890) {
        List<PaperTrackings> expectedTrackings = scenario.getExpected().getTrackings();
        if (expectedTrackings == null) return;
        assertEquals(expectedTrackings.size(), actualTrackings.size(), "Mismatch Trackings count");
        expectedTrackings.forEach(expected ->
                verifySingleTracking(scenario, expected, actualTrackings, ocrStatusEnum, strictFinalValidationStock890)
        );
    }

    private static void verifySingleTracking(ProductTestCase scenario, PaperTrackings expected, List<PaperTrackings> actualTrackings, OcrStatusEnum ocrStatusEnum, boolean strictFinalValidationStock890) {

        PaperTrackings actual = actualTrackings.stream()
                .filter(t -> t.getTrackingId().equalsIgnoreCase(expected.getTrackingId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Tracking not found: " + expected.getTrackingId()));

        verifyTrackingFields(expected, actual);
        verifyEvents(expected.getEvents(), actual.getEvents());
        verifyValidationConfig(expected.getValidationConfig(), actual.getValidationConfig(),
                ocrStatusEnum, strictFinalValidationStock890);
        verifyValidationFlow(scenario, expected, actual, ocrStatusEnum);
        if (StringUtils.isBlank(expected.getNextRequestIdPcretry()) && !scenario.getName().equalsIgnoreCase("NOT_RETRYABLE_EVENT_AR")
                && !scenario.getName().equalsIgnoreCase("NOT_RETRYABLE_EVENT_890")) {
            verifyPaperStatus(scenario.getName(), expected.getPaperStatus(), actual.getPaperStatus(), expected.getEvents(), expected, scenario.getExpected().getErrors());
        }

        assertNotNull(actual.getCreatedAt());
        assertNotNull(actual.getUpdatedAt());
    }

    private static void verifyTrackingFields(PaperTrackings exp, PaperTrackings act) {
        assertAll("Tracking fields",
                () -> assertEquals(exp.getBusinessState(), act.getBusinessState()),
                () -> assertEquals(exp.getAttemptId(), act.getAttemptId()),
                () -> assertEquals(exp.getProductType(), act.getProductType()),
                () -> assertEquals(exp.getState(), act.getState()),
                () -> assertEquals(exp.getProcessingMode(), act.getProcessingMode()),
                () -> assertEquals(exp.getUnifiedDeliveryDriver(), act.getUnifiedDeliveryDriver()),
                () -> assertEquals(exp.getNextRequestIdPcretry(), act.getNextRequestIdPcretry()),
                () -> assertEquals(exp.getPcRetry(), act.getPcRetry())
        );
    }

    private static void verifyEvents(List<Event> expected, List<Event> actual) {

        assertNotNull(actual);
        assertEquals(expected.size(), actual.size(), "Mismatch events count");

        // Ordiniamo gli eventi reali per createdAt (ordine di inserimento)
        List<Event> sortedActual = actual.stream()
                .sorted(Comparator.comparing(Event::getCreatedAt))
                .toList();

        for (int i = 0; i < expected.size(); i++) {

            Event expEvent = expected.get(i);
            Event actEvent = sortedActual.get(i);

            int finalI = i;
            assertAll("Event at position " + i,
                    () -> assertNotNull(actEvent.getId()),
                    () -> assertEquals(expEvent.getId(), actEvent.getId()),

                    () -> assertEquals(expEvent.getRequestTimestamp(), actEvent.getRequestTimestamp(),
                            "Mismatch requestTimestamp at index " + finalI),

                    () -> assertEquals(expEvent.getStatusCode(), actEvent.getStatusCode()),
                    () -> assertEquals(expEvent.getStatusTimestamp(), actEvent.getStatusTimestamp()),
                    () -> assertEquals(expEvent.getStatusDescription(), actEvent.getStatusDescription()),
                    () -> assertEquals(expEvent.getProductType(), actEvent.getProductType()),
                    () -> assertEquals(expEvent.getDeliveryFailureCause(), actEvent.getDeliveryFailureCause()),
                    () -> assertEquals(expEvent.getRegisteredLetterCode(), actEvent.getRegisteredLetterCode()),
                    () -> assertEquals(expEvent.getNotificationReworkId(), actEvent.getNotificationReworkId()),
                    () -> assertEquals(expEvent.getAnonymizedDiscoveredAddressId(), actEvent.getAnonymizedDiscoveredAddressId()),
                    () -> assertNotNull(actEvent.getCreatedAt())
            );

            verifyAttachments(expEvent.getAttachments(), actEvent.getAttachments());
        }
    }

    private static void verifyAttachments(List<Attachment> expected, List<Attachment> actual) {
        assertEquals(expected.size(), actual.size(), "Mismatch attachments count");

        expected.forEach(expAtt -> {

            Attachment actAtt = actual.stream()
                    .filter(a -> a.getDocumentType().equalsIgnoreCase(expAtt.getDocumentType())
                            && a.getId().equalsIgnoreCase(expAtt.getId()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Attachment not found: " + expAtt));

            assertAll("Attachment fields",
                    () -> assertNotNull(actAtt.getId()),
                    () -> assertEquals(expAtt.getDocumentType(), actAtt.getDocumentType())
            );
        });
    }

    private static void verifyValidationConfig(ValidationConfig expected, ValidationConfig actual, OcrStatusEnum ocrStatusEnum, boolean strictFinalValidationStock890) {

        assertNotNull(actual);

        assertAll("ValidationConfig",
                () -> assertEquals(ocrStatusEnum, actual.getOcrEnabled()),
                () -> assertEquals(expected.getRequiredAttachmentsRefinementStock890(), actual.getRequiredAttachmentsRefinementStock890()),
                () -> assertEquals(expected.getSendOcrAttachmentsFinalValidation(), actual.getSendOcrAttachmentsFinalValidation()),
                () -> assertEquals(expected.getSendOcrAttachmentsFinalValidationStock890(), actual.getSendOcrAttachmentsFinalValidationStock890()),
                () -> assertEquals(expected.getSendOcrAttachmentsRefinementStock890(), actual.getSendOcrAttachmentsRefinementStock890()),
                () -> assertEquals(strictFinalValidationStock890, actual.getStrictFinalValidationStock890())
        );
    }

    private static void verifyValidationFlow(ProductTestCase scenario, PaperTrackings expected, PaperTrackings actual, OcrStatusEnum ocrStatusEnum) {

        ValidationFlow flow = actual.getValidationFlow();
        ValidationFlow expectedFlow = expected.getValidationFlow();
        String scenarioName = scenario.getName();

        boolean isOkRs = scenarioName.equalsIgnoreCase("OK_RS");
        boolean isOkRis = scenarioName.equalsIgnoreCase("OK_RIS");
        boolean isOkRetryRs = scenarioName.equalsIgnoreCase("OK_RETRY_RS")
                || scenarioName.equalsIgnoreCase("OK_RETRY_M10_RS");
        boolean isCausaForzaMaggioreRS = scenarioName.equalsIgnoreCase("CAUSA_FORZA_MAGGIORE_RS");
        boolean isOkNonRendicontabileRs = scenarioName.equalsIgnoreCase("OK_NON_RENDICONTABILE_RS");
        boolean isOkRetryRis = scenarioName.equalsIgnoreCase("OK_RETRY_RIS")
                || scenarioName.equalsIgnoreCase("OK_RETRY_M10_RIS");
        boolean isM10Retry = scenarioName.equalsIgnoreCase("OK_RETRY_M10_AR")
                || scenarioName.equalsIgnoreCase("OK_RETRY_M10_RS")
                || scenarioName.equalsIgnoreCase("OK_RETRY_M10_RIR")
                || scenarioName.equalsIgnoreCase("OK_RETRY_M10_RIS")
                || scenarioName.equalsIgnoreCase("OK_RETRY_M10_890");
        boolean isFailCompiuta = scenarioName.equalsIgnoreCase("FAIL_COMPIUTA_GIACENZA_AR") || scenarioName.equalsIgnoreCase("FAIL_COMPIUTA_GIACENZA_AR_2");
        boolean isOcrPending = scenarioName.equalsIgnoreCase("OK_AR_OCR_PENDING") || scenarioName.equalsIgnoreCase("OK_890_OCR_PENDING");
        boolean isGiacenza890 = scenarioName.equalsIgnoreCase("OK_GIACENZA_EMPTY_REGISTEREDLETTERCODE_KO_890");
        boolean noRetry = StringUtils.isBlank(expected.getNextRequestIdPcretry());
        boolean hasRetry = !noRetry;
        boolean businessDoneNoRetry = expected.getBusinessState() == BusinessState.DONE && noRetry;
        boolean stateDoneNoRetry = expected.getState() == DONE && noRetry;
        boolean isOkRsRis = isCausaForzaMaggioreRS || isOkRs || isOkRis || ((isOkRetryRs || isOkRetryRis || isOkNonRendicontabileRs) && noRetry);

        if (isM10Retry && hasRetry) {
            assertNotNull(flow.getFinalEventBuilderTimestamp());
            assertNotNull(flow.getFinalEventDematValidationTimestamp());
            assertNotNull(flow.getSequencesValidationTimestamp());
        } else if (isOkRsRis) {
            assertNotNull(flow.getFinalEventBuilderTimestamp());
            assertNull(flow.getFinalEventDematValidationTimestamp());
            assertNotNull(flow.getSequencesValidationTimestamp());
        } else if (businessDoneNoRetry) {
            assertNotNull(flow.getFinalEventBuilderTimestamp());
            assertNotNull(flow.getFinalEventDematValidationTimestamp());
            assertNotNull(flow.getSequencesValidationTimestamp());
        } else if (isFailCompiuta) {
            assertNull(flow.getFinalEventBuilderTimestamp());
            assertNotNull(flow.getFinalEventDematValidationTimestamp());
            assertNotNull(flow.getSequencesValidationTimestamp());
        } else if (isOcrPending) {
            assertNull(flow.getFinalEventBuilderTimestamp());
            assertNull(flow.getFinalEventDematValidationTimestamp());
            assertNotNull(flow.getSequencesValidationTimestamp());
        } else {
            assertNull(flow.getFinalEventBuilderTimestamp());
            assertNull(flow.getFinalEventDematValidationTimestamp());
            assertNull(flow.getSequencesValidationTimestamp());
        }


        boolean hasRecag012 = scenario.getEvents().stream().anyMatch(e -> Objects.nonNull(e.getAnalogMail())
                        && "RECAG012".equalsIgnoreCase(e.getAnalogMail().getStatusCode()));

        if (hasRecag012) {
            assertNotNull(flow.getRecag012StatusTimestamp());
        } else {
            assertNull(flow.getRecag012StatusTimestamp());
        }

        if (isOkRsRis) {
            assertNull(flow.getRefinementDematValidationTimestamp());

        } else if ((isM10Retry && hasRetry) || stateDoneNoRetry || isFailCompiuta || isGiacenza890) {
            assertNotNull(flow.getRefinementDematValidationTimestamp());

        } else {
            assertNull(flow.getRefinementDematValidationTimestamp());
        }

        verifyOcrRequests(expectedFlow, flow, ocrStatusEnum, hasRetry,
                expected.getState() == DONE || expected.getBusinessState() == BusinessState.DONE, scenarioName, scenario.getExpected().getSentToOcr());
    }

    private static void verifyOcrRequests(ValidationFlow expected, ValidationFlow actual, OcrStatusEnum ocrStatusEnum, boolean hasNextRequestIdPcretry, boolean isDone, String testCase, Integer sentToOcr) {
        boolean isFailCompiutaGiacenzaAr = testCase.equalsIgnoreCase("FAIL_COMPIUTA_GIACENZA_AR") || testCase.equalsIgnoreCase("FAIL_COMPIUTA_GIACENZA_AR_2");
        boolean isOkGiacenzaEmptyRegisteredLetterCode890 = testCase.equalsIgnoreCase("OK_GIACENZA_EMPTY_REGISTEREDLETTERCODE_KO_890");
        boolean isOcrPending = testCase.equalsIgnoreCase("OK_AR_OCR_PENDING") || testCase.equalsIgnoreCase("OK_890_OCR_PENDING");

        if (ocrStatusEnum == OcrStatusEnum.DISABLED || sentToOcr == 0) {
            assertTrue(actual.getOcrRequests().isEmpty());
            return;
        }

        if (expected != null && CollectionUtils.isEmpty(expected.getOcrRequests())) {
            assertTrue(actual.getOcrRequests().isEmpty());
            return;
        }

        boolean isM10RetryInitialTracking = testCase.startsWith("OK_RETRY_M10_") && hasNextRequestIdPcretry;
        if ((!isM10RetryInitialTracking && hasNextRequestIdPcretry) || (!isDone && !isFailCompiutaGiacenzaAr && !isOkGiacenzaEmptyRegisteredLetterCode890 && !isOcrPending)) {
            assertTrue(actual.getOcrRequests().isEmpty());
            return;
        }

        if (testCase.contains("ZIP")) {
            assertTrue(actual.getOcrRequests().isEmpty());
            return;
        }
        assertFalse(actual.getOcrRequests().isEmpty());
        assertEquals(expected.getOcrRequests().size(), actual.getOcrRequests().size());

        expected.getOcrRequests().forEach(exp -> {
            OcrRequest act = actual.getOcrRequests().stream()
                    .filter(o -> o.getFinalEventId().equalsIgnoreCase(exp.getFinalEventId())
                            && o.getDocumentType().equalsIgnoreCase(exp.getDocumentType())
                            && o.getAttachmentEventId().equalsIgnoreCase(exp.getAttachmentEventId()))
                    .findFirst()
                    .orElseThrow();

            assertNotNull(act.getRequestTimestamp());
            assertNotNull(act.getResponseTimestamp());
            assertNotNull(act.getResponseStatus());
            Assertions.assertEquals(exp.getResponseStatus(), act.getResponseStatus());
        });
    }

    private static void verifyPaperStatus(String testCase, PaperStatus exp, PaperStatus act, List<Event> events, PaperTrackings expectedTracking, List<PaperTrackingsErrors> expectedErrors) {

        boolean isDone = BusinessState.DONE.equals(expectedTracking.getBusinessState());
        boolean isFailCompiutaGiacenzaAr = "FAIL_COMPIUTA_GIACENZA_AR".equalsIgnoreCase(testCase) || "FAIL_COMPIUTA_GIACENZA_AR_2".equalsIgnoreCase(testCase);
        boolean isOkTimestampError890 = "OK_TIMESTAMPERROR_890".equalsIgnoreCase(testCase);
        boolean isOcrPending = "OK_AR_OCR_PENDING".equalsIgnoreCase(testCase) || "OK_890_OCR_PENDING".equalsIgnoreCase(testCase);

        boolean isProduct890 = "890".equalsIgnoreCase(expectedTracking.getProductType());
        boolean isStrictFinalValidation890False =
                Objects.nonNull(expectedTracking.getValidationConfig().getStrictFinalValidationStock890()) &&
                        !expectedTracking.getValidationConfig().getStrictFinalValidationStock890();

        boolean hasDateError =
                !CollectionUtils.isEmpty(expectedErrors) &&
                        expectedErrors.stream()
                                .anyMatch(error -> ErrorCategory.DATE_ERROR.equals(error.getErrorCategory()));

        boolean hasP000Event =
                events.stream()
                        .anyMatch(e -> "P000".equalsIgnoreCase(e.getStatusCode()));

        assertAll("PaperStatus",
                () -> assertEquals(exp.getRegisteredLetterCode(), act.getRegisteredLetterCode()),
                () -> assertEquals(exp.getDeliveryFailureCause(), act.getDeliveryFailureCause()),
                () -> assertEquals(exp.getAnonymizedDiscoveredAddress(), act.getAnonymizedDiscoveredAddress()),
                () -> assertEquals(exp.getFinalStatusCode(), act.getFinalStatusCode()),
                () -> assertEquals(exp.getFinalDematFound(), act.getFinalDematFound()),
                () -> assertEquals(exp.getPredictedRefinementType(), act.getPredictedRefinementType()),

                // validatedSequenceTimestamp
                () -> {
                    if (!isDone && !isFailCompiutaGiacenzaAr && !isOcrPending) {
                        assertNull(act.getValidatedSequenceTimestamp());
                    } else if (isOkTimestampError890) {
                        assertNotNull(act.getValidatedSequenceTimestamp());
                    } else if (isProduct890
                            && isStrictFinalValidation890False
                            && hasDateError) {
                        assertNull(act.getValidatedSequenceTimestamp());
                    } else {
                        assertNotNull(act.getValidatedSequenceTimestamp());
                    }
                },

                // paperDeliveryTimestamp
                () -> {
                    if (hasP000Event) {
                        assertNotNull(act.getPaperDeliveryTimestamp());
                    } else {
                        assertNull(act.getPaperDeliveryTimestamp());
                    }
                },

                // validatedEvents
                () -> assertEquals(
                        Optional.ofNullable(exp.getValidatedEvents())
                                .map(HashSet::new)
                                .orElseGet(HashSet::new),
                        Optional.ofNullable(act.getValidatedEvents())
                                .map(HashSet::new)
                                .orElseGet(HashSet::new)
                )
        );
    }
}
