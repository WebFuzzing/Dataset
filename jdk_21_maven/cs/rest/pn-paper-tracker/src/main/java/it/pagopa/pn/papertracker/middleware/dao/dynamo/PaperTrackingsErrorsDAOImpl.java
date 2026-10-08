package it.pagopa.pn.papertracker.middleware.dao.dynamo;

import it.pagopa.pn.commons.log.dto.metrics.GeneralMetric;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import it.pagopa.pn.papertracker.utils.MetricUtils;
import lombok.CustomLog;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Instant;
import java.util.List;

@Component
@CustomLog
public class PaperTrackingsErrorsDAOImpl extends BaseDao<PaperTrackingsErrors> implements PaperTrackingsErrorsDAO {

    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;

    public PaperTrackingsErrorsDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnPaperTrackerConfigs cfg, DynamoDbAsyncClient dynamoDbAsyncClient) {
        super(dynamoDbEnhancedClient,
                dynamoDbAsyncClient,
                cfg.getDao().getPaperTrackingsErrorsTable(),
                PaperTrackingsErrors.class
        );
        this.pnPaperTrackerConfigs = cfg;
    }

    public Mono<PaperTrackingsErrors> insertError(PaperTrackingsErrors paperTrackingsErrors) {
        String logMessage = "Logging metrics: " + MetricUtils.MetricName.ERROR_COUNT +"; Dimensions: " + MetricUtils.DimensionName.ERROR_CATEGORY.getValue() + ", " + MetricUtils.DimensionName.PRODUCT_TYPE.getValue();
        GeneralMetric errorCountMetric = MetricUtils.generateGeneralMetric(
                MetricUtils.MetricName.ERROR_COUNT,
                1,
                List.of(
                        MetricUtils.generateDimension(MetricUtils.DimensionName.ERROR_CATEGORY, paperTrackingsErrors.getErrorCategory().name()),
                        MetricUtils.generateDimension(MetricUtils.DimensionName.PRODUCT_TYPE, paperTrackingsErrors.getProductType())
                )
        );

        log.logMetric(List.of(errorCountMetric), logMessage);

        paperTrackingsErrors.setTtl(Instant.now().plus(pnPaperTrackerConfigs.getPaperTrackingsErrorsTtlDuration()).getEpochSecond());
        return putItem(paperTrackingsErrors);
    }

    public Flux<PaperTrackingsErrors> retrieveErrors(String requestId) {
        return retrieveByRequestId(requestId);
    }
}
