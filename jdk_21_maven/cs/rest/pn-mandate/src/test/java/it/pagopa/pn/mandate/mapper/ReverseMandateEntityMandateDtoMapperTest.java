package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDtoRequest;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.UserDto;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.utils.DateUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

class ReverseMandateEntityMandateDtoMapperTest extends AbstractTestConfiguration {

    @Autowired
    ReverseMandateEntityMandateDtoMapper mapper;

    @Test
    void toEntity_withValidDate_shouldSetValidTo() {
        MandateDtoRequest dto = new MandateDtoRequest();
        dto.setDateto("2023-10-10");
        MandateEntity entity = mapper.toEntity(dto);
        assertNotNull(entity.getValidto());
        assertEquals(DateUtils.atStartOfDay(DateUtils.parseDate("2023-10-10").toInstant()).plusDays(1).minusSeconds(1).toInstant(), entity.getValidto());
    }

    @Test
    void toEntity_withDelegator_shouldSetDelegatorIsPerson() {
        MandateDtoRequest dto = new MandateDtoRequest();
        dto.setDelegator(new UserDto().person(true));
        MandateEntity entity = mapper.toEntity(dto);
        assertTrue(entity.getDelegatorisperson());
    }

    @Test
    void toEntity_generateRandomCode_shouldReturnFiveDigitString() {
        MandateDtoRequest dto = new MandateDtoRequest();
        MandateEntity entity = mapper.toEntity(dto);
        String code = entity.getValidationcode();
        assertNotNull(code);
        assertEquals(5, code.length());
        assertTrue(code.matches("\\d{5}"));
    }

}
