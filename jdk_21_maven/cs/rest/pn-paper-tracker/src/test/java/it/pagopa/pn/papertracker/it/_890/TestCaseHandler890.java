package it.pagopa.pn.papertracker.it._890;

import it.pagopa.pn.papertracker.it.GenericTestCaseHandlerImpl;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.OcrEventHandler;
import it.pagopa.pn.papertracker.service.impl.NotificationReworkServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class TestCaseHandler890 extends GenericTestCaseHandlerImpl {

    public TestCaseHandler890(ExternalChannelHandler externalChannelHandler, PaperTrackingsErrorsDAO paperTrackingsErrorsDAO, PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDao, PaperTrackingsDAO paperTrackingsDAO, OcrEventHandler ocrEventHandler, NotificationReworkServiceImpl notificationReworkService) {
        super(externalChannelHandler, paperTrackingsErrorsDAO, paperTrackerDryRunOutputsDao, paperTrackingsDAO, ocrEventHandler, notificationReworkService);
    }

    @Override
    public String getProductType() {
        return ProductType._890.getValue();
    }
}
