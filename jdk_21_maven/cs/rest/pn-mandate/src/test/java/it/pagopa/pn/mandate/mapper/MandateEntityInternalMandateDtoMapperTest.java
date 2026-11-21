package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.InternalMandateDto;
import it.pagopa.pn.mandate.model.WorkFlowType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MandateEntityInternalMandateDtoMapperTest extends AbstractTestConfiguration {

    @Autowired
    MandateEntityInternalMandateDtoMapper mapper;

    @Test
    void toEntity() {
        try {
            mapper.toEntity(null);
            fail("no UnsupportedOperationException throw, write test");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    void toDto() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);


        //When
        InternalMandateDto result = mapper.toDto(mandateToInsert);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertNotNull(result.getMandateId());
            Assertions.assertNotNull(result.getDatefrom());
            Assertions.assertNotNull(result.getDateto());
            Assertions.assertNotNull(result.getDelegator());
            Assertions.assertNotNull(result.getDelegate());

            Assertions.assertEquals(mandateToInsert.getMandateId(),  result.getMandateId());
            Assertions.assertEquals(mandateToInsert.getValidfrom().toString(),  result.getDatefrom());
            Assertions.assertEquals(mandateToInsert.getValidto().toString(),  result.getDateto());
            Assertions.assertEquals(mandateToInsert.getDelegator(),  result.getDelegator());
            Assertions.assertEquals(mandateToInsert.getDelegate(),  result.getDelegate());
            Assertions.assertEquals(mandateToInsert.getVisibilityIds()==null?0:mandateToInsert.getVisibilityIds().size(),  result.getVisibilityIds().size());


        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void toDtoVisibilityIds() {
        //Given
        MandateEntity mandate = MandateDaoIT.newMandate(true);
        mandate.setVisibilityIds(Set.of("G"));

        //When
        InternalMandateDto result = mapper.toDto(mandate);

        //Then
        assertNotNull(result);
        assertNotNull(result.getVisibilityIds());
        assertTrue(result.getVisibilityIds().contains("G"));
    }

    @Test
    void toDtoWorkflowType() {
        //Given
        MandateEntity mandate = MandateDaoIT.newMandate(true);
        mandate.setWorkflowType(WorkFlowType.STANDARD);

        //When
        InternalMandateDto result = mapper.toDto(mandate);

        //Then
        assertNotNull(result);
        assertNotNull(result.getWorkflowType());
        assertEquals("STANDARD", result.getWorkflowType().toString());
    }

    @Test
    void toDtoIuns() {
        //Given
        MandateEntity mandate = MandateDaoIT.newMandate(true);
        mandate.setIuns(Set.of("IUN1", "IUN2"));

        //When
        InternalMandateDto result = mapper.toDto(mandate);

        //Then
        assertNotNull(result);
        assertNotNull(result.getIuns());
        assertEquals(2, result.getIuns().size());
    }
}