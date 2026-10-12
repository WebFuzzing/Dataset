package it.pagopa.pn.papertracker.mapper;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.config.TrackerConfigUtils;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.Tracking;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.FileType;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.NONE)
public class PaperTrackingsMapper {

    public static PaperTrackings toPaperTrackings(TrackingCreationRequest trackingCreationRequest,
                                                  TrackerConfigUtils trackerConfigUtils,
                                                  PnPaperTrackerConfigs pnPaperTrackerConfigs,
                                                  Instant now,
                                                  String xOriginClientId) {
        ProductType productType = ProductType.fromValue(trackingCreationRequest.getProductType());
        LocalDate localDate = LocalDate.now(ZoneId.of("Europe/Rome"));
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId(String.join(".", trackingCreationRequest.getAttemptId(), trackingCreationRequest.getPcRetry()));
        paperTrackings.setUnifiedDeliveryDriver(trackingCreationRequest.getUnifiedDeliveryDriver());
        paperTrackings.setProductType(trackingCreationRequest.getProductType());
        paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
        paperTrackings.setBusinessState(BusinessState.AWAITING_FINAL_STATUS_CODE);
        paperTrackings.setAttemptId(trackingCreationRequest.getAttemptId());
        paperTrackings.setPcRetry(trackingCreationRequest.getPcRetry());
        paperTrackings.setCreatedAt(now);
        paperTrackings.setProcessingMode(trackerConfigUtils.getActualProductsProcessingModes(localDate).get(productType));
        paperTrackings.setAnalogRequestClientId(xOriginClientId);
        PaperStatus paperStatus = new PaperStatus();
        paperTrackings.setPaperStatus(paperStatus);
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setOcrRequests(List.of());
        paperTrackings.setValidationFlow(validationFlow);
        ValidationConfig validationConfig = new ValidationConfig();
        validationConfig.setOcrFilterTemporal(pnPaperTrackerConfigs.getOcrFilterTemporal());
        validationConfig.setOcrFilterUnifiedDeliveryDriver(pnPaperTrackerConfigs.getOcrFilterUnifiedDeliveryDriver());
        validationConfig.setOcrEnabled(evaluateIfOcrIsEnabled(trackerConfigUtils, productType, now, trackingCreationRequest, localDate));
        validationConfig.setOcrFileTypes(pnPaperTrackerConfigs.getEnableOcrValidationForFile().stream().map(FileType::getValue).toList());
        validationConfig.setRequiredAttachmentsRefinementStock890(trackerConfigUtils.getActualRequiredAttachmentsRefinementStock890(localDate));
        validationConfig.setSendOcrAttachmentsRefinementStock890(trackerConfigUtils.getActualSendOcrAttachmentsRefinementStock890(localDate));
        validationConfig.setSendOcrAttachmentsFinalValidation(trackerConfigUtils.getActualSendOcrAttachmentsFinalValidation(localDate));
        validationConfig.setSendOcrAttachmentsFinalValidationStock890(trackerConfigUtils.getActualSendOcrAttachmentsFinalValidationStock890(localDate));
        validationConfig.setStrictFinalValidationStock890(trackerConfigUtils.getActualStrictFinalValidationStock890(localDate));
        validationConfig.setStrictDeliveryFailureCause(trackerConfigUtils.getActualStrictDeliveryFailureCause(localDate));
        paperTrackings.setValidationConfig(validationConfig);
        return paperTrackings;
    }

    private static OcrStatusEnum evaluateIfOcrIsEnabled(TrackerConfigUtils trackerConfigUtils,
                                                        ProductType productType,
                                                        Instant now,
                                                        TrackingCreationRequest trackingCreationRequest,
                                                        LocalDate localDate) {

        OcrStatusEnum ocrStatusEnum = trackerConfigUtils.getActualEnableOcrValidationFor(localDate).get(productType);

        if (Objects.isNull(ocrStatusEnum) || OcrStatusEnum.DISABLED.equals(ocrStatusEnum)) {
            return OcrStatusEnum.DISABLED;
        }

        if (OcrStatusEnum.DRY.equals(ocrStatusEnum)) {
            return OcrStatusEnum.DRY;
        }

        // Siamo in modalità OCR:RUN

        boolean isTemporalFilterConfigured = !trackerConfigUtils.isOcrFilterTemporalDisabled();
        boolean isUnifiedDeliveryDriverFilterConfigured = !trackerConfigUtils.isOcrFilterDriverDisabled();

        // Caso 1: entrambi i filtri disattivi
        if (!isTemporalFilterConfigured && !isUnifiedDeliveryDriverFilterConfigured) {
            log.info("Both OCR filters are disabled. OCR will be enabled in RUN mode");
            return OcrStatusEnum.RUN;
        }

        boolean isTemporalFilterActive = isTemporalFilterConfigured && trackerConfigUtils.isOcrFilterTemporalActive(now);
        boolean isUnifiedDeliveryDriverFilterActive = isUnifiedDeliveryDriverFilterConfigured &&
                trackerConfigUtils.isOcrFilterDriverActive(trackingCreationRequest.getUnifiedDeliveryDriver());

        log.info("Temporal filter is active: {}", isTemporalFilterActive);
        log.info("UnifiedDeliveryDriver {} is filtered: {}", trackingCreationRequest.getUnifiedDeliveryDriver(), isUnifiedDeliveryDriverFilterActive);

        // Caso 2: entrambi i filtri sono configurati
        if (isTemporalFilterConfigured && isUnifiedDeliveryDriverFilterConfigured) {
            return (isTemporalFilterActive && isUnifiedDeliveryDriverFilterActive) ? OcrStatusEnum.RUN : OcrStatusEnum.DRY;
        }

        // Caso 3: solo uno dei due filtri è configurato
        return (isTemporalFilterActive || isUnifiedDeliveryDriverFilterActive) ? OcrStatusEnum.RUN : OcrStatusEnum.DRY;
    }

}
