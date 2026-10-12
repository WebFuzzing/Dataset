package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerBadRequestException;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.SequenceItem;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.SequenceResponse;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.BusinessState;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperStatus;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState;
import it.pagopa.pn.papertracker.model.EventStatus;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfig;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfiguration;
import it.pagopa.pn.papertracker.service.NotificationReworkService;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;


@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationReworkServiceImpl implements NotificationReworkService {

    private final PaperTrackingsDAO paperTrackingsDAO;

    private static final String ERROR_CODE_INVALID_STATUS_CODE = "PN_PAPER_TRACKER_INVALID_STATUS_CODE";
    private static final String ERROR_CODE_INVALID_DELIVERY_FAILURE_CAUSE = "PN_PAPER_TRACKER_INVALID_STATUS_CODE";


    @Override
    public Mono<SequenceResponse> retrieveSequenceAndEventStatus(String statusCode, String deliveryFailureCause, String productType) {
        SequenceResponse sequenceResponse = new SequenceResponse();

        return Mono.justOrEmpty(TrackerUtility.evaluateStatusCodeAndRetrieveStatus(statusCode, deliveryFailureCause, productType))
                .switchIfEmpty(Mono.error(new PnPaperTrackerBadRequestException(ERROR_CODE_INVALID_STATUS_CODE, String.format("statusCode %s is invalid", statusCode))))
                .filter(eventStatus -> !EventStatus.PROGRESS.equals(eventStatus))
                .switchIfEmpty(Mono.error(new PnPaperTrackerBadRequestException(ERROR_CODE_INVALID_STATUS_CODE, String.format("statusCode %s is PROGRESS", statusCode))))
                .doOnNext(eventStatus -> sequenceResponse.setFinalStatusCode(SequenceResponse.FinalStatusCodeEnum.fromValue(eventStatus.name())))
                .flatMap(eventStatus -> Mono.justOrEmpty(retrieveSequence(statusCode, deliveryFailureCause)))
                .switchIfEmpty(Mono.error(new PnPaperTrackerBadRequestException(ERROR_CODE_INVALID_DELIVERY_FAILURE_CAUSE, String.format("deliveryFailureCause %s is invalid", deliveryFailureCause))))
                .map(sequenceList -> {
                    sequenceResponse.setSequence(sequenceList);
                    log.info("Successfully retrieved sequence for statusCode {} with {} elements", statusCode, sequenceList.size());
                    return sequenceResponse;
                })
                .doOnError(throwable -> log.error("Error retrieving sequence for statusCode {}: {}", statusCode, throwable.getMessage()));
    }

    @Override
    public Mono<Void> updatePaperTrackingsStatusForRework(String trackingId, String reworkId) {
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setState(PaperTrackingsState.AWAITING_REWORK_EVENTS);
        paperTrackings.setBusinessState(BusinessState.AWAITING_REWORK_EVENTS);
        paperTrackings.setNotificationReworkRequestTimestamp(Instant.now());
        paperTrackings.setNotificationReworkId(reworkId);
        return paperTrackingsDAO.updateItemForRework(trackingId, paperTrackings).then();
    }

    private List<SequenceItem> retrieveSequence(String statusCode, String deliveryFailureCause) {
        SequenceConfig sequenceConfig = SequenceConfiguration.getConfig(statusCode);
        List<String> deliveryFailureCauses = sequenceConfig.sequenceStatusCodes().stream()
                .map(EventStatusCodeEnum::fromKey)
                .flatMap(eventStatusCodeEnum -> eventStatusCodeEnum.getDeliveryFailureCauseList().stream().map(Enum::name))
                .toList();

        boolean hasFailureCause = StringUtils.isNotBlank(deliveryFailureCause);

        if (CollectionUtils.isEmpty(deliveryFailureCauses) ? hasFailureCause : !deliveryFailureCauses.contains(deliveryFailureCause)) {
            return null;
        }
        return sequenceConfig.sequenceStatusCodes().stream()
                .map(code -> {
                    SequenceItem item = new SequenceItem();
                    item.setStatusCode(code);
                    item.setAttachments(Optional.ofNullable(sequenceConfig.validAttachments().get(code)).orElse(Set.of()).stream().toList());
                    return item;
                }).toList();
    }
}


