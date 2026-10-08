package it.pagopa.pn.papertracker.mapper;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.config.TrackerConfigUtils;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.Tracking;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class PaperTrackingsMapperTest {

    private final String xOriginClientId = "clientId";

    @Spy
    private PaperTrackerMapStructMapper mapper = Mappers.getMapper(PaperTrackerMapStructMapper.class);

    @Test
    void toPaperTrackingsValidRequest() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("driver456");
        request.setProductType("RS");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request,trackerConfigUtils, pnPaperTrackerConfigs, Instant.now(), xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("driver456", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.RS.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.DRY, paperTrackings.getProcessingMode());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsInvalidProductType() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("driver456");
        request.setProductType("INVALID_TYPE");

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(new PnPaperTrackerConfigs());

        //ACT & ASSERT
        assertThrows(IllegalArgumentException.class, () -> PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, new PnPaperTrackerConfigs(), Instant.now(), xOriginClientId));
    }

    @Test
    void toPaperTrackingsOCRfiltersBothDisabled() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("DISABLED");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("disabled")); //controllo sul minuscolo

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, Instant.now(), xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.RUN, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledAndActive() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("* * 14,16-18 * * MON,FRI");
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));

        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("POSTE"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        Instant dateTime = Instant.parse("2026-02-27T15:00:00.333Z"); //venerdì 27 febbraio 2026 alle 16:00 ora italiana (UTC+1)

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, dateTime, xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.RUN, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledTemporalActive() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("* * 14,16-18 * * MON,FRI");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("Sailpost"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        Instant dateTime = Instant.parse("2026-02-27T15:00:00.333Z"); //venerdì 27 febbraio 2026 alle 16:00 ora italiana (UTC+1)

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, dateTime, xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.DRY, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledDriverActive() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("* * 14,16-18 * * MON,TUE");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("POSTE"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        Instant dateTime = Instant.parse("2026-02-27T15:00:00.333Z"); //venerdì 27 febbraio 2026 alle 16:00 ora italiana (UTC+1)

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, dateTime, xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.DRY, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledTemporalDisabledDriverActive() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("DISABLED");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("POSTE"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, Instant.now(), xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.RUN, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledDriverDisabledTemporalActive() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:RUN;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("* * 16-20 * * FRI");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("DISABLED"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        Instant dateTime = Instant.parse("2026-02-27T19:15:08.333Z"); //venerdì 27 febbraio 2026 alle 20:15 ora italiana (UTC+1)

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, dateTime, xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.RUN, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledDRYmode() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;AR:DRY;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("* * 16-20 * * FRI");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("POSTE"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        Instant dateTime = Instant.parse("2026-02-27T19:15:08.333Z"); //venerdì 27 febbraio 2026 alle 20:15 ora italiana (UTC+1)

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, dateTime, xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.DRY, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void toPaperTrackingsOCRfiltersBothEnabledDisabledMode() {
        //ARRANGE
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("POSTE");
        request.setProductType("AR");

        PnPaperTrackerConfigs pnPaperTrackerConfigs = new PnPaperTrackerConfigs();
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidationStock890(List.of("1970-01-01;ARCAD"));
        pnPaperTrackerConfigs.setRequiredAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsRefinementStock890(List.of("1970-01-01;23L"));
        pnPaperTrackerConfigs.setSendOcrAttachmentsFinalValidation(List.of("1970-01-01;Plico;AR;23L"));
        pnPaperTrackerConfigs.setStrictFinalValidationStock890(List.of("1970-01-01;true"));
        pnPaperTrackerConfigs.setStrictDeliveryFailureCause(List.of("1970-01-01;false"));
        pnPaperTrackerConfigs.setEnableOcrValidationFor(List.of("1970-01-01;RIR:RUN;890:RUN"));
        pnPaperTrackerConfigs.setProductsProcessingModes(List.of("1970-01-01;AR:RUN;RS:DRY"));
        pnPaperTrackerConfigs.setOcrFilterTemporal("* * 16-20 * * FRI");
        pnPaperTrackerConfigs.setOcrFilterUnifiedDeliveryDriver(List.of("POSTE"));

        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        Instant dateTime = Instant.parse("2026-02-27T19:15:08.333Z"); //venerdì 27 febbraio 2026 alle 20:15 ora italiana (UTC+1)

        //ACT
        PaperTrackings paperTrackings = PaperTrackingsMapper.toPaperTrackings(request, trackerConfigUtils, pnPaperTrackerConfigs, dateTime, xOriginClientId);

        //ASSERT
        Assertions.assertEquals("request123.PCRETRY_0", paperTrackings.getTrackingId());
        Assertions.assertEquals("request123", paperTrackings.getAttemptId());
        Assertions.assertEquals("PCRETRY_0", paperTrackings.getPcRetry());
        Assertions.assertEquals("POSTE", paperTrackings.getUnifiedDeliveryDriver());
        Assertions.assertEquals(ProductType.AR.getValue(), paperTrackings.getProductType());
        Assertions.assertEquals(ProcessingMode.RUN, paperTrackings.getProcessingMode());
        Assertions.assertEquals(OcrStatusEnum.DISABLED, paperTrackings.getValidationConfig().getOcrEnabled());
        Assertions.assertEquals(xOriginClientId, paperTrackings.getAnalogRequestClientId());
    }

    @Test
    void entityToOutputTest(){
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("tracking123");
        paperTrackings.setAttemptId("attempt123");
        paperTrackings.setPcRetry("PCRETRY_1");
        paperTrackings.setProductType(ProductType._890.getValue());
        paperTrackings.setUnifiedDeliveryDriver("driver789");
        paperTrackings.setProcessingMode(ProcessingMode.DRY);

        Attachment attachment = new Attachment();
        attachment.setUri("uri");
        attachment.setId("id");
        attachment.setSha256("sha256");
        attachment.setDate(Instant.now());
        attachment.setDocumentType("ARCAD");
        attachment.setSourceType("SCANNED");
        attachment.setOriginType("ORIGINAL");

        Event event = new Event();
        event.setId("id");
        event.setIun("iun");
        event.setRequestTimestamp(Instant.now());
        event.setStatusCode("RECAG001A");
        event.setStatusDescription("description");
        event.setStatusTimestamp(Instant.now());
        event.setProductType(ProductType._890.getValue());
        event.setDeliveryFailureCause("M06");
        event.setAnonymizedDiscoveredAddressId("ADDR");
        event.setAttachments(List.of(attachment));
        event.setRegisteredLetterCode("regLetterCode123");
        event.setDryRun(true);
        event.setCreatedAt(Instant.now());

        paperTrackings.setEvents(List.of(event));

        PaperStatus paperStatus = new PaperStatus();
        paperStatus.setRegisteredLetterCode("regLetterCode123");
        paperStatus.setDeliveryFailureCause("M01");
        paperStatus.setAnonymizedDiscoveredAddress("ADDR");
        paperStatus.setFinalStatusCode("RECAG003C");
        paperStatus.setValidatedSequenceTimestamp(Instant.now());
        paperStatus.setPaperDeliveryTimestamp(Instant.now());

        paperStatus.setValidatedEvents(List.of("event1","event2"));
        paperStatus.setFinalDematFound(true);
        paperTrackings.setPaperStatus(paperStatus);

        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setSequencesValidationTimestamp(Instant.now());
        validationFlow.setFinalEventDematValidationTimestamp(Instant.now());
        validationFlow.setRefinementDematValidationTimestamp(Instant.now());
        validationFlow.setFinalEventBuilderTimestamp(Instant.now());
        validationFlow.setRecag012StatusTimestamp(Instant.now());
        OcrRequest ocrRequest = new OcrRequest();
        ocrRequest.setRequestTimestamp(Instant.now());
        ocrRequest.setResponseTimestamp(Instant.now());
        ocrRequest.setDocumentType("23L");
        ocrRequest.setFinalEventId("eventId");
        ocrRequest.setAttachmentEventId("attachmentEventId");
        ocrRequest.setResponseStatus("OK");
        ocrRequest.setUri("uri");
        validationFlow.setOcrRequests(List.of(ocrRequest));
        paperTrackings.setValidationFlow(validationFlow);

        ValidationConfig validationConfig = new ValidationConfig();
        validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
        validationConfig.setRequiredAttachmentsRefinementStock890(List.of("23L"));
        validationConfig.setSendOcrAttachmentsFinalValidationStock890(List.of("ARCAD"));
        validationConfig.setSendOcrAttachmentsFinalValidation(List.of("23L","ARCAD"));
        validationConfig.setStrictFinalValidationStock890(true);
        validationConfig.setStrictDeliveryFailureCause(true);
        validationConfig.setSendOcrAttachmentsRefinementStock890(List.of("23L"));
        validationConfig.setOcrFilterTemporal("* * 9-18 * * MON");
        validationConfig.setOcrFilterUnifiedDeliveryDriver(List.of("POSTE"));
        paperTrackings.setValidationConfig(validationConfig);

        paperTrackings.setNextRequestIdPcretry("nextRequestId123");
        paperTrackings.setState(PaperTrackingsState.DONE);
        paperTrackings.setBusinessState(BusinessState.AWAITING_FINAL_STATUS_CODE);
        paperTrackings.setCreatedAt(Instant.now());
        paperTrackings.setUpdatedAt(Instant.now());

        Tracking tracking = mapper.toTracking(paperTrackings);

        Assertions.assertEquals(paperTrackings.getTrackingId(), tracking.getTrackingId());
        Assertions.assertEquals(paperTrackings.getAttemptId(), tracking.getAttemptId());
        Assertions.assertEquals(paperTrackings.getPcRetry(), tracking.getPcRetry());
        Assertions.assertEquals(paperTrackings.getProductType(), tracking.getProductType());
        Assertions.assertEquals(paperTrackings.getUnifiedDeliveryDriver(), tracking.getUnifiedDeliveryDriver());
        Assertions.assertEquals(paperTrackings.getEvents().size(), tracking.getEvents().size());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getId(), tracking.getEvents().getFirst().getId());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getStatusCode(), tracking.getEvents().getFirst().getStatusCode());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getStatusTimestamp(), tracking.getEvents().getFirst().getStatusTimestamp());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getStatusDescription(), tracking.getEvents().getFirst().getStatusDescription());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getIun(), tracking.getEvents().getFirst().getIun());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getRegisteredLetterCode(), tracking.getEvents().getFirst().getRegisteredLetterCode());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getRequestTimestamp(), tracking.getEvents().getFirst().getRequestTimestamp());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getProductType(), Objects.requireNonNull(tracking.getEvents().getFirst().getProductType()));
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getDeliveryFailureCause(), tracking.getEvents().getFirst().getDeliveryFailureCause());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getAnonymizedDiscoveredAddressId(), tracking.getEvents().getFirst().getAnonymizedDiscoveredAddressId());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getDryRun(), tracking.getEvents().getFirst().getDryRun());
        Assertions.assertNotNull(tracking.getEvents().getFirst().getCreatedAt());
        Assertions.assertEquals(paperTrackings.getEvents().getFirst().getAttachments().size(), Objects.requireNonNull(tracking.getEvents().getFirst().getAttachments()).size());
        it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.Attachment trackingAttachment = tracking.getEvents().getFirst().getAttachments().getFirst();
        Attachment entityAttachment = paperTrackings.getEvents().getFirst().getAttachments().getFirst();
        Assertions.assertEquals(entityAttachment.getId(), trackingAttachment.getId());
        Assertions.assertEquals(entityAttachment.getUri(), trackingAttachment.getUri());
        Assertions.assertEquals(entityAttachment.getSha256(), trackingAttachment.getSha256());
        Assertions.assertEquals(entityAttachment.getDate(), trackingAttachment.getDate());
        Assertions.assertEquals(entityAttachment.getDocumentType(), trackingAttachment.getDocumentType());
        Assertions.assertEquals(entityAttachment.getSourceType(), trackingAttachment.getSourceType());
        Assertions.assertEquals(entityAttachment.getOriginType(), trackingAttachment.getOriginType());


        Assertions.assertNotNull(tracking.getPaperStatus());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getRegisteredLetterCode(), tracking.getPaperStatus().getRegisteredLetterCode());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getDeliveryFailureCause(), tracking.getPaperStatus().getDeliveryFailureCause());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getAnonymizedDiscoveredAddress(), tracking.getPaperStatus().getAnonymizedDiscoveredAddress());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getFinalStatusCode(), tracking.getPaperStatus().getFinalStatusCode());
        Assertions.assertNotNull(tracking.getPaperStatus().getValidatedSequenceTimestamp());
        Assertions.assertNotNull(tracking.getPaperStatus().getPaperDeliveryTimestamp());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getRegisteredLetterCode(), tracking.getPaperStatus().getRegisteredLetterCode());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getRegisteredLetterCode(), tracking.getPaperStatus().getRegisteredLetterCode());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getRegisteredLetterCode(), tracking.getPaperStatus().getRegisteredLetterCode());
        Assertions.assertNotNull(tracking.getPaperStatus().getValidatedEvents());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getValidatedEvents().size(), tracking.getPaperStatus().getValidatedEvents().size());
        Assertions.assertEquals(paperTrackings.getPaperStatus().getFinalDematFound(), tracking.getPaperStatus().getFinalDematFound());


        Assertions.assertNotNull(tracking.getValidationFlow());
        Assertions.assertNotNull(tracking.getValidationFlow().getSequencesValidationTimestamp());
        Assertions.assertNotNull(tracking.getValidationFlow().getFinalEventDematValidationTimestamp());
        Assertions.assertNotNull(tracking.getValidationFlow().getRefinementDematValidationTimestamp());
        Assertions.assertNotNull(tracking.getValidationFlow().getFinalEventBuilderTimestamp());
        Assertions.assertNotNull(tracking.getValidationFlow().getRecag012StatusTimestamp());
        Assertions.assertNotNull(tracking.getValidationFlow().getOcrRequests());
        Assertions.assertEquals(paperTrackings.getValidationFlow().getOcrRequests().getFirst().getDocumentType(), tracking.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
        Assertions.assertEquals(paperTrackings.getValidationFlow().getOcrRequests().getFirst().getFinalEventId(), tracking.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        Assertions.assertEquals(paperTrackings.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId(), tracking.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        Assertions.assertEquals(paperTrackings.getValidationFlow().getOcrRequests().getFirst().getResponseStatus(), tracking.getValidationFlow().getOcrRequests().getFirst().getResponseStatus());
        Assertions.assertEquals(paperTrackings.getValidationFlow().getOcrRequests().getFirst().getUri(), tracking.getValidationFlow().getOcrRequests().getFirst().getUri());
        Assertions.assertNotNull(tracking.getValidationFlow().getOcrRequests().getFirst().getResponseTimestamp());
        Assertions.assertNotNull(tracking.getValidationFlow().getOcrRequests().getFirst().getRequestTimestamp());

        Assertions.assertNotNull(tracking.getValidationConfig());
        Assertions.assertEquals(paperTrackings.getValidationConfig().getOcrEnabled().name(), tracking.getValidationConfig().getOcrEnabled().name());
        Assertions.assertEquals(validationConfig.getSendOcrAttachmentsFinalValidation(), tracking.getValidationConfig().getSendOcrAttachmentsFinalValidation());
        Assertions.assertEquals(validationConfig.getRequiredAttachmentsRefinementStock890(), tracking.getValidationConfig().getRequiredAttachmentsRefinementStock890());
        Assertions.assertEquals(validationConfig.getSendOcrAttachmentsFinalValidationStock890(), tracking.getValidationConfig().getSendOcrAttachmentsFinalValidationStock890());
        Assertions.assertEquals(validationConfig.getStrictFinalValidationStock890(), tracking.getValidationConfig().getStrictFinalValidationStock890());
        Assertions.assertEquals(validationConfig.getStrictDeliveryFailureCause(), tracking.getValidationConfig().getStrictDeliveryFailureCause());
        Assertions.assertEquals(validationConfig.getSendOcrAttachmentsRefinementStock890(), tracking.getValidationConfig().getSendOcrAttachmentsRefinementStock890());
        Assertions.assertEquals(validationConfig.getOcrFilterTemporal(), tracking.getValidationConfig().getOcrFilterTemporal());
        Assertions.assertEquals(validationConfig.getOcrFilterUnifiedDeliveryDriver(), tracking.getValidationConfig().getOcrFilterUnifiedDeliveryDriver());

        Assertions.assertEquals(paperTrackings.getNextRequestIdPcretry(), tracking.getNextRequestIdPcretry());
        Assertions.assertEquals(paperTrackings.getState().name(), tracking.getState().name());
        Assertions.assertEquals(paperTrackings.getBusinessState().name(), tracking.getBusinessState().name());
        Assertions.assertEquals(paperTrackings.getProcessingMode() != null ? paperTrackings.getProcessingMode().name() : null,
                tracking.getProcessingMode() != null ? tracking.getProcessingMode().name() : null);
        Assertions.assertNotNull(tracking.getCreatedAt());
        Assertions.assertNotNull(tracking.getUpdatedAt());
    }

}
