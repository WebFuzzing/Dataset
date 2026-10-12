package it.pagopa.pn.papertracker.mapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Attachment;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutput;
import org.mapstruct.factory.Mappers;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PaperTrackerDryRunOutputsMapperTest {

    @Spy
    private PaperTrackerMapStructMapper mapper = Mappers.getMapper(PaperTrackerMapStructMapper.class);

    @Test
    void shouldSetAndGetAllFieldsCorrectly() {
        PaperTrackerDryRunOutputs entity = new PaperTrackerDryRunOutputs();
        entity.setCreated(Instant.now());
        entity.setRegisteredLetterCode("RL123");
        entity.setStatusCode("RECRN001A");
        entity.setStatusDetail("Detail");
        entity.setStatusDescription("Description");
        entity.setStatusDateTime("2024-06-01T12:00:00Z");
        entity.setDeliveryFailureCause("M01");
        Attachment attachment = new Attachment();
        attachment.setId("A1");
        attachment.setDocumentType("Plico");
        attachment.setDate(Instant.now());
        attachment.setUri("url");
        attachment.setSha256("sha256");
        entity.setAttachments(List.of(attachment));
        entity.setAnonymizedDiscoveredAddressId("ADDR1");
        entity.setClientRequestTimestamp("2024-06-01T12:01:00Z");
        PaperTrackerOutput output = mapper.toDtoPaperTrackerOutput(entity);

        Assertions.assertEquals("RL123", output.getRegisteredLetterCode());
        Assertions.assertNotNull(output.getCreated());
        Assertions.assertEquals("RECRN001A", output.getStatusCode());
        Assertions.assertEquals("Detail", output.getStatusDetail());
        Assertions.assertEquals("Description", output.getStatusDescription());
        Assertions.assertEquals("2024-06-01T12:00:00Z", output.getStatusDateTime());
        Assertions.assertEquals("M01", output.getDeliveryFailureCause());
        Assertions.assertNotNull(output.getAttachments());
        Assertions.assertEquals(1, output.getAttachments().size());
        Assertions.assertEquals("A1", output.getAttachments().getFirst().getId());
        Assertions.assertEquals("Plico", output.getAttachments().getFirst().getDocumentType());
        Assertions.assertEquals("url", output.getAttachments().getFirst().getUri());
        Assertions.assertNotNull(output.getAttachments().getFirst().getDate());
        Assertions.assertEquals("ADDR1", output.getAnonymizedDiscoveredAddressId());
        Assertions.assertNotNull(output.getClientRequestTimestamp());
    }
}

