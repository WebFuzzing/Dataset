package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.middleware.db.entities.DelegateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateCountsDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.fail;

class UserEntityMandateCountsDtoMapperTest extends AbstractTestConfiguration {

    @Autowired
    UserEntityMandateCountsDtoMapper mapper;

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
        DelegateEntity delegateToInsert = new DelegateEntity();
        delegateToInsert.setPendingcount(5);


        //When
        MandateCountsDto result = mapper.toDto(delegateToInsert);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertNotNull(result.getValue());

            Assertions.assertEquals(delegateToInsert.getPendingcount(),  result.getValue());
        } catch (Exception e) {
            fail(e);
        }
    }
}