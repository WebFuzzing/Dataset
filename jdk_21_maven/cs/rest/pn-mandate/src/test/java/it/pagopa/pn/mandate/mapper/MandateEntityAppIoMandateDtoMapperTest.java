package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationResponse;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class MandateEntityAppIoMandateDtoMapperTest {

    private final MandateEntityAppIoMandateDtoMapper mapper = new MandateEntityAppIoMandateDtoMapper();

    @Test
    void toDto_shouldMapFieldsCorrectly() {
        MandateEntity entity = new MandateEntity();
        entity.setMandateId("mandateId");
        entity.setValidto(Instant.ofEpochSecond(1234567890));
        entity.setValidationcode("CODE123");

        MandateCreationResponse response = mapper.toDto(entity);

        assertNotNull(response);
        assertNotNull(response.getMandate());
        assertEquals("mandateId", response.getMandate().getMandateId());
        assertEquals("CODE123", response.getMandate().getVerificationCode());
        assertEquals(1234567890, response.getRequestTTL());
        assertEquals(String.valueOf(entity.getValidto()), response.getMandate().getDateTo());
    }

    @Test
    void toEntity_shouldThrowUnsupportedOperationException() {
       MandateCreationResponse response = new MandateCreationResponse();
       assertThrows(UnsupportedOperationException.class, () -> mapper.toEntity(response));
    }
}
