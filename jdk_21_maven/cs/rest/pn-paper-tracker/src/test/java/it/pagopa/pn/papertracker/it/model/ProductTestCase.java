package it.pagopa.pn.papertracker.it.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductTestCase {
    private String name;
    private String productType;
    private TrackingCreationRequest initialTracking;
    private List<TestEvent> events;
    private boolean createAttempt1;
    private Expected expected;
    private PcRetryResponse firstPcRetryResponse;
    private PcRetryResponse secondPcRetryResponse;

}
