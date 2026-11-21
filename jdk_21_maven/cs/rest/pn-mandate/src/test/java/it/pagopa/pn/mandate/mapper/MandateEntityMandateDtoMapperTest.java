package it.pagopa.pn.mandate.mapper;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.OrganizationIdDto;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.utils.DateUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;

class MandateEntityMandateDtoMapperTest extends AbstractTestConfiguration {

    @Autowired
    MandateEntityMandateDtoMapper mapper;

    @Test
    void toEntity() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        MandateDto dto = mapper.toDto(mandateToInsert);

        //When
        MandateEntity result = mapper.toEntity(dto);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertNotNull(result.getMandateId());
            Assertions.assertNotNull(result.getValidto());
            Assertions.assertNotNull(result.getValidationcode());

            Assertions.assertEquals(mandateToInsert.getMandateId(),  result.getMandateId());
            Assertions.assertEquals(mandateToInsert.getValidto().atZone(ZoneId.of("Europe/Rome")).getDayOfYear(),  result.getValidto().atZone(ZoneId.of("Europe/Rome")).getDayOfYear());
            Assertions.assertEquals(mandateToInsert.getValidationcode(),  result.getValidationcode());
            // implicano la risoluzione da datavault, non ha senso testarli
            //Assertions.assertEquals(mandateToInsert.getDelegateisperson(),  result.getDelegate().getPerson());
            //Assertions.assertEquals(mandateToInsert.getDelegatorisperson(),  result.getDelegator().getPerson());
            Assertions.assertEquals(mandateToInsert.getVisibilityIds()==null?0:mandateToInsert.getVisibilityIds().size(),
                        result.getVisibilityIds()==null?0:result.getVisibilityIds().size());

        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void toEntityNoStatus() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        MandateDto dto = mapper.toDto(mandateToInsert);
        dto.setStatus(null);

        //When
        MandateEntity result = mapper.toEntity(dto);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(0 , result.getState());
        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void toEntityNoDelegate() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        MandateDto dto = mapper.toDto(mandateToInsert);
        dto.setDelegate(null);

        //When
        MandateEntity result = mapper.toEntity(dto);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertNull(result.getDelegate());
        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void toEntityNoDelegator() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        MandateDto dto = mapper.toDto(mandateToInsert);
        dto.setDelegator(null);

        //When
        MandateEntity result = mapper.toEntity(dto);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertNull(result.getDelegator());
        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void toEntityWithList() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        MandateDto dto = mapper.toDto(mandateToInsert);
        List<OrganizationIdDto> list = new ArrayList<>();
        OrganizationIdDto organizationIdDto = new OrganizationIdDto();
        organizationIdDto.setName("prova");
        organizationIdDto.setUniqueIdentifier("123");
        list.add(organizationIdDto);
        dto.setVisibilityIds(list);

        //When
        MandateEntity result = mapper.toEntity(dto);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(dto.getVisibilityIds().size(), result.getVisibilityIds().size());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void toDto() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        mandateToInsert.setGroups(Set.of("G"));

        //When
        MandateDto result = mapper.toDto(mandateToInsert);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertNotNull(result.getMandateId());
            Assertions.assertNotNull(result.getDatefrom());
            Assertions.assertNotNull(result.getDateto());
            Assertions.assertNotNull(result.getDelegator());
            Assertions.assertNotNull(result.getDelegate());
            Assertions.assertNotNull(result.getStatus());
            Assertions.assertNotNull(result.getVerificationCode());
            Assertions.assertNotNull(result.getGroups());


            Assertions.assertEquals(mandateToInsert.getMandateId(),  result.getMandateId());
            Assertions.assertEquals(DateUtils.formatDate(mandateToInsert.getValidfrom()),  result.getDatefrom());
            Assertions.assertEquals(DateUtils.formatDate(mandateToInsert.getValidto()),  result.getDateto());
            Assertions.assertEquals(mandateToInsert.getState(), StatusEnumMapper.intValfromStatus(result.getStatus()));
            Assertions.assertEquals(mandateToInsert.getValidationcode(),  result.getVerificationCode());
            Assertions.assertEquals(mandateToInsert.getDelegateisperson(),  result.getDelegate().getPerson());
            Assertions.assertEquals(mandateToInsert.getDelegatorisperson(),  result.getDelegator().getPerson());
            Assertions.assertEquals(mandateToInsert.getVisibilityIds()==null?0:mandateToInsert.getVisibilityIds().size(),  result.getVisibilityIds().size());
            Assertions.assertEquals(mandateToInsert.getGroups() == null ? 0 : mandateToInsert.getGroups().size(), result.getGroups().size());

        } catch (Exception e) {
            fail(e);
        }
    }


    @Test
    void toDtoWithList() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        Set<String> ids = new HashSet<>();
        ids.add("123");
        mandateToInsert.setVisibilityIds(ids);

        //When
        MandateDto result = mapper.toDto(mandateToInsert);

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(mandateToInsert.getVisibilityIds().size(),  result.getVisibilityIds().size());
        } catch (Exception e) {
            fail(e);
        }
    }

}