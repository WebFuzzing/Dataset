package it.pagopa.pn.papertracker.middleware.queue;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.middleware.queue.model.DeliveryPushEvent;
import it.pagopa.pn.papertracker.middleware.queue.model.ExternalChannelEvent;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelOutputsMomProducer;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelToPaperChannelDryRunMomProducer;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelToPaperTrackerMomProducer;
import it.pagopa.pn.papertracker.middleware.queue.producer.OcrMomProducer;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@AllArgsConstructor
public class PnPaperTrackerMiddlewareConfigs {
    private PnPaperTrackerConfigs pnPaperChannelConfigs;

    @Bean
    public OcrMomProducer ocrMomProducer(ObjectMapper objMapper) {
        SqsClient sqsClient = SqsClient.builder()
                .region(Region.of(this.pnPaperChannelConfigs.getTopics().getQueueOcrInputsRegion()))
                .build();
        return new OcrMomProducer(sqsClient,
                this.pnPaperChannelConfigs.getTopics().getQueueOcrInputsUrl(),
                this.pnPaperChannelConfigs.getTopics().getQueueOcrInputsUrl(),
                objMapper,
                OcrEvent.class);
    }

    @Bean
    public ExternalChannelOutputsMomProducer externalChannelOutputsMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new ExternalChannelOutputsMomProducer(sqsClient,
                this.pnPaperChannelConfigs.getTopics().getExternalChannelOutputsQueue(),
                objMapper,
                DeliveryPushEvent.class);
    }

    @Bean
    public ExternalChannelToPaperTrackerMomProducer externalChannelToPaperTrackerMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new ExternalChannelToPaperTrackerMomProducer(sqsClient,
                this.pnPaperChannelConfigs.getTopics().getExternalChannelToPaperTrackerQueue(),
                objMapper,
                ExternalChannelEvent.class);
    }

    @Bean
    public ExternalChannelToPaperChannelDryRunMomProducer externalChannelToPaperChannelDryRunMomProducer(SqsClient sqsClient, ObjectMapper objMapper) {
        return new ExternalChannelToPaperChannelDryRunMomProducer(sqsClient,
                this.pnPaperChannelConfigs.getTopics().getExternalChannelToPaperChannelDryRunQueue(),
                objMapper,
                ExternalChannelEvent.class);
    }
}