package it.pagopa.pn.papertracker.it;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;

public interface GenericTestCaseHandler {

    String getProductType();

    void beforeInit(ProductTestCase scenario, boolean strictFinalValidation, OcrStatusEnum ocrStatusEnum);

    void afterInit(ProductTestCase scenario, TrackingCreationRequest request);

    void sendEventsAndOcrResponse(ProductTestCase scenario, OcrStatusEnum ocrStatusEnum);

    void afterSendEvents(ProductTestCase scenario, OcrStatusEnum ocrStatusEnum, boolean strictValidation);

}