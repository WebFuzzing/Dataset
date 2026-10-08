package it.pagopa.pn.papertracker.config;

import it.pagopa.pn.commons.conf.SharedAutoConfiguration;
import it.pagopa.pn.papertracker.model.FileType;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "pn.paper-tracker")
@Data
@Import({SharedAutoConfiguration.class})
@Slf4j
public class PnPaperTrackerConfigs {

    private Dao dao;
    private String paperChannelBaseUrl;
    private String dataVaultBaseUrl;
    private String safeStorageBaseUrl;
    public String safeStorageCxId;
    private Duration paperTrackingsTtlDuration;
    private Duration paperTrackingsErrorsTtlDuration;
    private Topics topics;
    private int maxPcRetryMock;

    private List<String> enableOcrValidationFor = new ArrayList<>();
    private List<FileType> enableOcrValidationForFile = new ArrayList<>();
    private List<String> saveAndNotSendToDeliveryPush = new ArrayList<>();
    private List<String> requiredAttachmentsRefinementStock890 = new ArrayList<>();
    private List<String> sendOcrAttachmentsRefinementStock890 = new ArrayList<>();
    private List<String> sendOcrAttachmentsFinalValidationStock890 = new ArrayList<>();
    private List<String> sendOcrAttachmentsFinalValidation = new ArrayList<>();
    private List<String> strictFinalValidationStock890;
    private List<String> strictDeliveryFailureCause;
    private List<String> internalEvents = new ArrayList<>();
    private List<String> productsProcessingModes = new ArrayList<>();
    private List<String> redriveEnabledDomains = new ArrayList<>();
    private List<String> sendToConsolidatoreErrorCategories = new ArrayList<>();
    private String ocrFilterTemporal;
    private List<String> ocrFilterUnifiedDeliveryDriver = new ArrayList<>();

    private Duration compiutaGiacenzaArDuration;
    private boolean enableTruncatedDateForRefinementCheck;
    private Duration refinementDuration;

    private EventBus eventBus;

    @Data
    public static class Dao {
        private String paperTrackingsErrorsTable;
        private String paperTrackerDryRunOutputsTable;
        private String paperTrackingsTable;
    }

    @Data
    public static class Topics {
        // Consumer + Producer
        private String externalChannelToPaperTrackerQueue;
        // Consumer
        private String externalChannelToPaperChannelQueue;
        private String pnOcrOutputsQueue;
        // Producer
        private String queueOcrInputsUrl;
        private String queueOcrInputsRegion;
        private String externalChannelOutputsQueue;
        private String externalChannelToPaperChannelDryRunQueue;
    }

    @Data
    public static class EventBus {
        private String name;
        private String detailType;
        private String source;
    }

    @PostConstruct
    public void init() {
        validateSendOcrAttachmentsFinalValidationStock890();

        log.info("CONFIGURATIONS: {}", this);
    }


    public void validateSendOcrAttachmentsFinalValidationStock890() {
        List<String> expected = List.of("1970-01-01;");

        if (!expected.equals(sendOcrAttachmentsFinalValidationStock890)) {
            throw new IllegalStateException(
                    "Invalid configuration for sendOcrAttachmentsFinalValidationStock890. " +
                            "Expected exactly: " + expected +
                            ", but found: " + sendOcrAttachmentsFinalValidationStock890
            );
        }
    }
}
