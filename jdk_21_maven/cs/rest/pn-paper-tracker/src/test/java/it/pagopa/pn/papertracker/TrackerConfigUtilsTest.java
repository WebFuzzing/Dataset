package it.pagopa.pn.papertracker;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.config.TrackerConfigUtils;
import it.pagopa.pn.papertracker.exception.ConfigNotFound;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProcessingMode;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
public class TrackerConfigUtilsTest {

    private final LocalDate startDate = LocalDate.of(2025, 3, 2);

    @Test
    void returnsRequiredAttachmentsRefinementStock890Empty() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setRequiredAttachmentsRefinementStock890(List.of("2023-01-01", "2026-01-01;DOC1;DOC2", "2027-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualRequiredAttachmentsRefinementStock890(startDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsRequiredAttachmentsRefinementStock890OneItems() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setRequiredAttachmentsRefinementStock890(List.of("2023-01-01", "2024-01-01;DOC1", "2027-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualRequiredAttachmentsRefinementStock890(startDate);

        assertEquals(1, result.size());
    }

    @Test
    void returnsRequiredAttachmentsRefinementStock890MoreItems() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setRequiredAttachmentsRefinementStock890(List.of("2023-01-01", "2024-01-01;DOC1", "2025-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualRequiredAttachmentsRefinementStock890(startDate);

        assertEquals(2, result.size());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationEmpty() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidation(List.of("2023-01-01", "2026-01-01;DOC1;DOC2", "2027-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidation(startDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationOneItems() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidation(List.of("2023-01-01", "2024-01-01;DOC1", "2027-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidation(startDate);

        assertEquals(1, result.size());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationMoreItems() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidation(List.of("2023-01-01", "2024-01-01;DOC1", "2025-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidation(startDate);

        assertEquals(2, result.size());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationStock890Empty() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidationStock890(List.of("2023-01-01", "2026-01-01;DOC1;DOC2", "2027-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidationStock890(startDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationStock890OneItems() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidationStock890(List.of("2023-01-01", "2024-01-01;DOC1", "2027-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidationStock890(startDate);

        assertEquals(1, result.size());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationStock890MoreItems() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidationStock890(List.of("2023-01-01", "2024-01-01;DOC1", "2025-01-01;DOC3;DOC4"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidationStock890(startDate);

        assertEquals(2, result.size());
    }

    @Test
    void returnsSendOcrAttachmentsFinalValidationStock890OnlyStartDate() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        List<String> result = utils.getActualSendOcrAttachmentsFinalValidationStock890(startDate);

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsStrictFinalValidationStock890Default() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setStrictFinalValidationStock890(List.of("2023-01-01", "2026-01-01;true", "2027-01-01;false"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Boolean result = utils.getActualStrictFinalValidationStock890(startDate);

        assertFalse(result);
    }

    @Test
    void returnsStrictFinalValidationStock890true() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setStrictFinalValidationStock890(List.of("2023-01-01", "2024-01-01;true", "2027-01-01;false"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Boolean result = utils.getActualStrictFinalValidationStock890(startDate);

        assertTrue(result);
    }

    @Test
    void returnsStrictFinalValidationStock890false() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setStrictFinalValidationStock890(List.of("2023-01-01", "2024-01-01;true", "2025-01-01;false"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Boolean result = utils.getActualStrictFinalValidationStock890(startDate);

        assertFalse(result);
    }

    @Test
    void returnsProductsProcessingModes() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RIR:RUN", "2026-02-02;AR:RUN;RIR:RUN;890:DRY"));
        Map<ProductType, ProcessingMode> resultExpected = Map.of(
                ProductType.AR, ProcessingMode.RUN,
                ProductType.RIR, ProcessingMode.RUN
        );
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Map<ProductType, ProcessingMode> result = utils.getActualProductsProcessingModes(startDate);

        assertEquals(resultExpected, result);
    }

    @Test
    void returnsProductsProcessingModesStartDateAfterTwo() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setProductsProcessingModes(List.of("2024-04-12;890:RUN", "2025-03-01;AR:RUN;RIR:RUN;890:DRY"));
        Map<ProductType, ProcessingMode> resultExpected = Map.of(
                ProductType.AR, ProcessingMode.RUN,
                ProductType.RIR, ProcessingMode.RUN,
                ProductType._890, ProcessingMode.DRY
        );
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Map<ProductType, ProcessingMode> result = utils.getActualProductsProcessingModes(startDate);

        assertEquals(resultExpected, result);
    }

    @Test
    void returnsProductsProcessingModesStartDateEqualToConfigDate() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setProductsProcessingModes(List.of("2026-08-19;AR:RUN", "2025-03-02;AR:DRY;RIR:RUN;890:RUN"));
        Map<ProductType, ProcessingMode> resultExpected = Map.of(
                ProductType.AR, ProcessingMode.DRY,
                ProductType.RIR, ProcessingMode.RUN,
                ProductType._890, ProcessingMode.RUN
        );
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Map<ProductType, ProcessingMode> result = utils.getActualProductsProcessingModes(startDate);

        assertEquals(resultExpected, result);
    }

    @Test
    void returnsProductsProcessingModesEmpty() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setProductsProcessingModes(List.of());
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);
        assertThrows(
                ConfigNotFound.class,
                () -> utils.getActualProductsProcessingModes(startDate)
        );
    }

    @Test
    void returnsProductsProcessingModesNotFound() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setProductsProcessingModes(List.of("2026-02-02;AR:RUN;RIR:RUN;890:DRY"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        assertThrows(
                ConfigNotFound.class,
                () -> utils.getActualProductsProcessingModes(startDate)
        );
    }

    @Test
    void returnsOcrFilterTemporalActiveInactive() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterTemporal("* * 09-11,16-18 * * WED");
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean resultActive1 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T17:45:00Z"));
        boolean resultActive2 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T17:59:59Z"));
        boolean resultActive3 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T15:00:00Z"));
        boolean resultActive4 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T15:00:01Z"));
        boolean resultActive5 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T08:00:00Z"));
        boolean resultActive6 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T10:59:59Z"));
        boolean resultInactive1 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T19:45:00Z"));
        boolean resultInactive2 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T14:59:59Z"));
        boolean resultInactive3 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T07:59:59Z"));
        boolean resultInactive4 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T11:00:00Z"));
        boolean resultInactive5 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T14:59:59Z"));
        boolean resultInactive6 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-25T18:00:00Z"));
        boolean resultDisabled = utils.isOcrFilterTemporalDisabled();

        assertTrue(resultActive1);
        assertTrue(resultActive2);
        assertTrue(resultActive3);
        assertTrue(resultActive4);
        assertTrue(resultActive5);
        assertTrue(resultActive6);
        assertFalse(resultInactive1);
        assertFalse(resultInactive2);
        assertFalse(resultInactive3);
        assertFalse(resultInactive4);
        assertFalse(resultInactive5);
        assertFalse(resultInactive6);
        assertFalse(resultDisabled);
    }

    @Test
    void returnsOcrFilterTemporalActiveInactiveAllDay(){
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterTemporal("* * * * * MON");
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean resultInactive1 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T22:59:59Z"));
        boolean resultInactive2 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-23T23:00:00Z"));
        boolean resultActive1 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T23:00:00Z"));
        boolean resultActive2 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T23:00:01Z"));
        boolean resultActive3 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-23T22:59:59Z"));
        boolean resultDisabled = utils.isOcrFilterTemporalDisabled();

        assertFalse(resultInactive1);
        assertFalse(resultInactive2);
        assertTrue(resultActive1);
        assertTrue(resultActive2);
        assertTrue(resultActive3);
        assertFalse(resultDisabled);
    }

    @Test
    void returnsOcrFilterTemporalActiveInactiveAllDay2(){
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterTemporal("* * * * * MON");
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean resultInactive1 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T22:59:59Z"));
        boolean resultInactive2 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T22:59:59.333Z"));
        boolean resultActive1 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T23:00:00.333Z"));
        boolean resultActive2 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T23:00:01Z"));
        boolean resultActive3 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T23:00:01.333Z"));
        boolean resultActive4 = utils.isOcrFilterTemporalActive(Instant.parse("2026-02-22T23:00:02.333Z"));

        assertFalse(resultInactive1);
        assertFalse(resultInactive2);
        assertTrue(resultActive1);
        assertTrue(resultActive2);
        assertTrue(resultActive3);
        assertTrue(resultActive4);
    }

    @Test
    void returnsOcrFilterTemporalActiveInactiveAllDay3(){
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterTemporal("* * * * * *");
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean resultActive1 = utils.isOcrFilterTemporalActive(Instant.now());

        assertTrue(resultActive1);

    }

    @Test
    void returnsOcrFilterTemporalDisabled() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterTemporal("DISABLED");
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean result = utils.isOcrFilterTemporalDisabled();

        assertTrue(result);
    }

    @Test
    void returnsOcrFilterDriverActiveInactive() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterUnifiedDeliveryDriver(List.of("DRIVER1", "DRIVER2"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean resultActive = utils.isOcrFilterDriverActive("DRIVER1");
        boolean resultInactive = utils.isOcrFilterDriverActive("DRIVER3");
        boolean resultDisabled = utils.isOcrFilterDriverDisabled();

        assertTrue(resultActive);
        assertFalse(resultInactive);
        assertFalse(resultDisabled);
    }

    @Test
    void returnsOcrFilterDriverDisabled() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setOcrFilterUnifiedDeliveryDriver(List.of("DISABLED"));
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        boolean result = utils.isOcrFilterDriverDisabled();

        assertTrue(result);
    }

    @Test
    void returnsEnableOcrValidationFor() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN", "2026-02-02;AR:RUN;RIR:RUN;890:DRY"));
        Map<ProductType, OcrStatusEnum> resultExpected = Map.of(
                ProductType.AR, OcrStatusEnum.RUN,
                ProductType.RIR, OcrStatusEnum.RUN
        );
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Map<ProductType, OcrStatusEnum> result = utils.getActualEnableOcrValidationFor(startDate);

        assertEquals(resultExpected, result);
    }

    @Test
    void returnsEnableOcrValidationForStartDateEqualToConfigDate() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setEnableOcrValidationFor(List.of("2026-08-19;AR:RUN", "2025-03-02;AR:DRY;RIR:RUN;890:RUN"));
        Map<ProductType, OcrStatusEnum> resultExpected = Map.of(
                ProductType.AR, OcrStatusEnum.DRY,
                ProductType.RIR, OcrStatusEnum.RUN,
                ProductType._890, OcrStatusEnum.RUN
        );
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);

        Map<ProductType, OcrStatusEnum> result = utils.getActualEnableOcrValidationFor(startDate);

        assertEquals(resultExpected, result);
    }

    @Test
    void returnsEnableOcrValidationForEmpty() {
        PnPaperTrackerConfigs cfg = new PnPaperTrackerConfigs();
        cfg.setEnableOcrValidationFor(List.of());
        TrackerConfigUtils utils = new TrackerConfigUtils(cfg);
        assertThrows(
                ConfigNotFound.class,
                () -> utils.getActualEnableOcrValidationFor(startDate)
        );
    }

}
