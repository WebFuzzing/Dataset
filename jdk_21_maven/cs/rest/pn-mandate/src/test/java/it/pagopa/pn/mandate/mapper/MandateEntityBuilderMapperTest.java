package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.config.PnMandateConfig;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.UserInfoQrCodeDto;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.utils.MandateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MandateEntityBuilderMapperTest {

    private MandateEntityBuilderMapper mapper;
    private PnMandateConfig config;
    private MandateUtils mandateUtils;

    @BeforeEach
    void setUp() {
        config = new PnMandateConfig();
        config.setCiePendingDuration(java.time.Duration.ofDays(1));
        mandateUtils = new MandateUtils();
        mapper = new MandateEntityBuilderMapper(config, mandateUtils);
    }

    @Test
    void buildMandateEntity_shouldMapFieldsCorrectly() {
        String delegatorUserId = "PF-delegator";
        String delegateUserId = "delegate";
        String mandateId = "mandateId";
        UserInfoQrCodeDto dto = new UserInfoQrCodeDto();
        dto.setIun("IUN123");

        MandateEntity entity = mapper.buildMandateEntity(delegatorUserId, dto, mandateId, delegateUserId);

        assertEquals(delegatorUserId, entity.getDelegator());
        assertEquals(delegateUserId, entity.getDelegate());
        assertEquals(mandateId, entity.getMandateId());
        assertEquals(Set.of("IUN123"), entity.getIuns());
        assertEquals("delegator", entity.getDelegatorUid());
        assertTrue(entity.getDelegatorisperson());
        assertTrue(entity.getDelegateisperson());
        assertNotNull(entity.getValidationcode());
        assertNotNull(entity.getCreated());
        assertNotNull(entity.getValidfrom());
        assertNotNull(entity.getValidto());
        assertEquals("IO", entity.getSrcChannel());
    }
}
