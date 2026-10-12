package it.pagopa.pn.papertracker.it.AR;

import it.pagopa.pn.papertracker.it.GenericTestCaseHandlerImpl;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.OcrEventHandler;
import it.pagopa.pn.papertracker.service.NotificationReworkService;
import it.pagopa.pn.papertracker.service.impl.NotificationReworkServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
public class TestCaseHandlerAR extends GenericTestCaseHandlerImpl {

    public TestCaseHandlerAR(ExternalChannelHandler externalChannelHandler, PaperTrackingsErrorsDAO paperTrackingsErrorsDAO, PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDao, PaperTrackingsDAO paperTrackingsDAO, OcrEventHandler ocrEventHandler, NotificationReworkService notificationReworkService) {
        super(externalChannelHandler, paperTrackingsErrorsDAO, paperTrackerDryRunOutputsDao, paperTrackingsDAO, ocrEventHandler, notificationReworkService);
    }

    @Override
    public String getProductType() {
        return ProductType.AR.getValue();
    }
}
