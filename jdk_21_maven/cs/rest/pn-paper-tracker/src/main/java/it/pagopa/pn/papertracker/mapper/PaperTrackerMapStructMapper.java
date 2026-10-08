package it.pagopa.pn.papertracker.mapper;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutput;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.Tracking;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingError;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.BusinessState;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorType;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.time.Instant;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        unmappedSourcePolicy = ReportingPolicy.IGNORE
)
public interface PaperTrackerMapStructMapper {

    @Mapping(target = "notificationReworkTimestamp", source = "notificationReworkRequestTimestamp")
    Tracking toTracking(PaperTrackings paperTrackings);

    TrackingError toTrackingError(PaperTrackingsErrors paperTrackingsErrors);

    PaperTrackerOutput toDtoPaperTrackerOutput(PaperTrackerDryRunOutputs entity);

    default String map(Instant instant) {
        return instant != null ? instant.toString() : null;
    }

    default Tracking.StateEnum map(PaperTrackingsState state) {
        if (state == null) {
            return null;
        }

        return switch (state) {
            case AWAITING_FINAL_STATUS_CODE, AWAITING_REFINEMENT -> Tracking.StateEnum.AWAITING_REFINEMENT;
            case AWAITING_REWORK_EVENTS -> Tracking.StateEnum.AWAITING_REWORK_EVENTS;
            case AWAITING_OCR -> Tracking.StateEnum.AWAITING_OCR;
            case DONE -> Tracking.StateEnum.DONE;
            case KO -> Tracking.StateEnum.KO;
        };
    }

    default Tracking.BusinessStateEnum map(BusinessState businessState) {
        if (businessState == null) {
            return null;
        }

        return switch (businessState) {
            case AWAITING_FINAL_STATUS_CODE -> Tracking.BusinessStateEnum.AWAITING_FINAL_STATUS_CODE;
            case AWAITING_REFINEMENT_OCR -> Tracking.BusinessStateEnum.AWAITING_OCR;
            case AWAITING_REWORK_EVENTS -> Tracking.BusinessStateEnum.AWAITING_REWORK_EVENTS;
            case AWAITING_OCR -> Tracking.BusinessStateEnum.AWAITING_REFINEMENT_OCR;
            case DONE -> Tracking.BusinessStateEnum.DONE;
            case KO -> Tracking.BusinessStateEnum.KO;
        };
    }

    default TrackingError.TypeEnum map(ErrorType errorType) {
        if (errorType == null) {
            return null;
        }

        return switch (errorType) {
            case ERROR -> TrackingError.TypeEnum.ERROR;
            case INFO, WARNING -> TrackingError.TypeEnum.WARNING;
        };
    }
}

