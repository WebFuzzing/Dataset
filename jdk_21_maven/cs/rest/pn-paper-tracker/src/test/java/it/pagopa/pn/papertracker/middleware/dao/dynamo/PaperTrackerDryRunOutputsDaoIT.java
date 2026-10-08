package it.pagopa.pn.papertracker.middleware.dao.dynamo;

import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Attachment;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

public class PaperTrackerDryRunOutputsDaoIT extends BaseTest.WithLocalStack {

    @Autowired
    PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDAO;

    @Test
    void insertAndRetrieveError() {
        //Arrange
        IntStream.range(0, 3).forEach(i -> {
            PaperTrackerDryRunOutputs output = new PaperTrackerDryRunOutputs();
            output.setTrackingId("requestId1");
            output.setCreated(Instant.now().minus(i, ChronoUnit.MINUTES));
            output.setClientRequestTimestamp(Instant.now().toString());
            output.setRegisteredLetterCode("registeredLetterCode");
            output.setDeliveryFailureCause("deliveryFailureCause");
            output.setAnonymizedDiscoveredAddressId("discoveredAddress");
            output.setStatusCode("RECRN001C");
            output.setStatusDetail("statusDetail");
            output.setStatusDescription("statusDescription");
            output.setStatusDateTime(Instant.now().toString());
            Attachment attachment = new Attachment();
            attachment.setId("id");
            attachment.setDocumentType("DOC");
            attachment.setDate(Instant.now());
            attachment.setUri("attachmentUrl");
            output.setAttachments(List.of(attachment));

            paperTrackerDryRunOutputsDAO.insertOutputEvent(output).block();
        });

        PaperTrackerDryRunOutputs output = new PaperTrackerDryRunOutputs();
        output.setTrackingId("requestId2");
        output.setCreated(Instant.now());
        output.setClientRequestTimestamp(Instant.now().toString());
        output.setRegisteredLetterCode("registeredLetterCode");
        output.setDeliveryFailureCause("deliveryFailureCause");
        output.setAnonymizedDiscoveredAddressId("discoveredAddress");
        output.setStatusCode("RECRN001C");
        output.setStatusDetail("statusDetail");
        output.setStatusDescription("statusDescription");
        output.setStatusDateTime(Instant.now().toString());
        Attachment attachment = new Attachment();
        attachment.setId("id");
        attachment.setDocumentType("DOC");
        attachment.setDate(Instant.now());
        attachment.setUri("attachmentUrl");
        output.setAttachments(List.of(attachment));

        paperTrackerDryRunOutputsDAO.insertOutputEvent(output).block();

        List<PaperTrackerDryRunOutputs> outputs = paperTrackerDryRunOutputsDAO.retrieveOutputEvents("requestId1").collectList().block();

        //Assert
        Assertions.assertNotNull(outputs);
        Assertions.assertEquals(3, outputs.size());
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getTrackingId().equals("requestId1")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getStatusCode().equals("RECRN001C")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getStatusDetail().equals("statusDetail")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getStatusDescription().equals("statusDescription")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getDeliveryFailureCause().equals("deliveryFailureCause")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getAnonymizedDiscoveredAddressId().equals("discoveredAddress")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getRegisteredLetterCode().equals("registeredLetterCode")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getCreated() != null));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getClientRequestTimestamp() != null));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getStatusDateTime() != null));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getAttachments() != null && !e.getAttachments().isEmpty()));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getAttachments().getFirst().getId().equals("id")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getAttachments().getFirst().getDocumentType().equals("DOC")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getAttachments().getFirst().getUri().equals("attachmentUrl")));
        Assertions.assertTrue(outputs.stream().allMatch(e -> e.getAttachments().getFirst().getDate() != null));

        List<PaperTrackerDryRunOutputs> outputs2 = paperTrackerDryRunOutputsDAO.retrieveOutputEvents("requestId2").collectList().block();
        Assertions.assertNotNull(outputs2);
        Assertions.assertEquals(1, outputs2.size());
        Assertions.assertEquals("requestId2", outputs2.getFirst().getTrackingId());
        Assertions.assertNotNull(outputs2.getFirst().getCreated());
        Assertions.assertNotNull(outputs2.getFirst().getClientRequestTimestamp());
        Assertions.assertEquals("registeredLetterCode", outputs2.getFirst().getRegisteredLetterCode());
        Assertions.assertEquals("deliveryFailureCause", outputs2.getFirst().getDeliveryFailureCause());
        Assertions.assertEquals("discoveredAddress", outputs2.getFirst().getAnonymizedDiscoveredAddressId());
        Assertions.assertEquals("RECRN001C", outputs2.getFirst().getStatusCode());
        Assertions.assertEquals("statusDetail", outputs2.getFirst().getStatusDetail());
        Assertions.assertEquals("statusDescription", outputs2.getFirst().getStatusDescription());
        Assertions.assertNotNull(outputs2.getFirst().getStatusDateTime());
        Assertions.assertNotNull(outputs2.getFirst().getAttachments());
        Assertions.assertEquals(1, outputs2.getFirst().getAttachments().size());
        Assertions.assertEquals("id", outputs2.getFirst().getAttachments().getFirst().getId());
        Assertions.assertEquals("DOC", outputs2.getFirst().getAttachments().getFirst().getDocumentType());
        Assertions.assertEquals("attachmentUrl", outputs2.getFirst().getAttachments().getFirst().getUri());
        Assertions.assertNotNull(outputs2.getFirst().getAttachments().getFirst().getDate());


    }
}
