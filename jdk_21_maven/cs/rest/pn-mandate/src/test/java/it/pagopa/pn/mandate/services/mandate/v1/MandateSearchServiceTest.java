package it.pagopa.pn.mandate.services.mandate.v1;

import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.PaSummaryDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.GroupDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.OrganizationIdDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.UserDto;
import it.pagopa.pn.mandate.mapper.MandateEntityMandateDtoMapper;
import it.pagopa.pn.mandate.middleware.db.MandateDao;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.middleware.db.PnLastEvaluatedKey;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.msclient.PnDataVaultClient;
import it.pagopa.pn.mandate.middleware.msclient.PnExtRegPrvtClient;
import it.pagopa.pn.mandate.middleware.msclient.PnInfoPaClient;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.model.PageResultDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ContextConfiguration(classes = {MandateSearchService.class})
@ExtendWith(SpringExtension.class)
class MandateSearchServiceTest {

    private static final Duration D = Duration.ofMillis(3000);

    @MockitoBean
    private MandateDao mandateDao;
    @MockitoBean
    private MandateEntityMandateDtoMapper mandateEntityMandateDtoMapper;
    @MockitoBean
    private PnDataVaultClient pnDataVaultClient;
    @MockitoBean
    private PnInfoPaClient pnInfoPaClient;
    @MockitoBean
    private PnExtRegPrvtClient pnExtRegPrvtClient;

    @Autowired
    private MandateSearchService mandateSearchService;

    @Test
    void testSearchByDelegate() {
        InputSearchMandateDto searchDto = InputSearchMandateDto.builder()
                .delegateId("delegateId")
                .size(2)
                .maxPageNumber(1)
                .build();

        MandateEntity entity1 = MandateDaoIT.newMandate(false);
        entity1.setVisibilityIds(Set.of("paId1", "paId2"));
        MandateEntity entity2 = MandateDaoIT.newMandate(false);
        entity2.setDelegator(entity1.getDelegator());

        Page<MandateEntity> page1 = Page.create(List.of(entity1), Map.of("k", AttributeValue.builder().s("v").build()));
        Page<MandateEntity> page2 = Page.create(List.of(entity2));
        when(mandateDao.searchByDelegate(eq("delegateId"), isNull(), isNull(), isNull(), eq(3), any()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2));

        BaseRecipientDtoDto recipientDto1 = new BaseRecipientDtoDto();
        recipientDto1.setInternalId(entity1.getDelegator());
        recipientDto1.setDenomination("denomination");
        recipientDto1.setTaxId("taxId");
        when(pnDataVaultClient.getRecipientDenominationByInternalId(List.of(entity1.getDelegator())))
                .thenReturn(Flux.fromIterable(List.of(recipientDto1)));

        PaSummaryDto paSummaryDto = new PaSummaryDto();
        paSummaryDto.setId("paId1");
        paSummaryDto.setName("paName");
        when(pnInfoPaClient.getManyPa(anyList()))
                .thenReturn(Flux.just(paSummaryDto));

        PgGroupDto pgGroupDto = new PgGroupDto();
        pgGroupDto.setId("pgGroupId");
        pgGroupDto.setName("pgGroupName");
        when(pnExtRegPrvtClient.getGroups("delegateId", false))
                .thenReturn(Flux.just(pgGroupDto));

        MandateDto dto1 = new MandateDto();
        dto1.setDelegator(new UserDto());
        OrganizationIdDto org1 = new OrganizationIdDto();
        org1.setUniqueIdentifier("paId1");
        OrganizationIdDto org2 = new OrganizationIdDto();
        org2.setUniqueIdentifier("paId2");
        dto1.setVisibilityIds(List.of(org1, org2));
        GroupDto groupDto = new GroupDto();
        groupDto.setId("pgGroupId");
        dto1.setGroups(List.of(groupDto));
        MandateDto dto2 = new MandateDto();
        dto2.setDelegator(new UserDto());
        when(mandateEntityMandateDtoMapper.toDto(entity1))
                .thenReturn(dto1);
        when(mandateEntityMandateDtoMapper.toDto(entity2))
                .thenReturn(dto2);

        PageResultDto<MandateDto, String> resultDto = mandateSearchService.searchByDelegate(searchDto, null)
                .block(D);
        assertNotNull(resultDto);
        assertEquals(2, resultDto.getPage().size());
        assertFalse(resultDto.isMore());
        assertTrue(resultDto.getNextPagesKey().isEmpty());
        assertEquals("denomination", resultDto.getPage().get(0).getDelegator().getDisplayName());
        assertEquals("taxId", resultDto.getPage().get(0).getDelegator().getFiscalCode());
        assertEquals("paName", resultDto.getPage().get(0).getVisibilityIds().get(0).getName());
        assertEquals("pgGroupName", resultDto.getPage().get(0).getGroups().get(0).getName());
    }

    @Test
    void testSearchByDelegateWithFilters() {
        InputSearchMandateDto searchDto = InputSearchMandateDto.builder()
                .delegateId("delegateId")
                .groups(List.of("G"))
                .delegatorIds(List.of("delegatorId"))
                .statuses(List.of(10, 20, 30))
                .size(1)
                .maxPageNumber(2)
                .build();

        PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey("20");
        lastEvaluatedKey.setInternalLastEvaluatedKey(Map.of("k", AttributeValue.builder().s("v").build()));

        MandateEntity entity1 = MandateDaoIT.newMandate(false);
        MandateEntity entity2 = MandateDaoIT.newMandate(false);

        Page<MandateEntity> page1 = Page.create(Collections.emptyList());
        Page<MandateEntity> page2 = Page.create(List.of(entity1, entity2), Map.of("k", AttributeValue.builder().s("v").build()));
        when(mandateDao.searchByDelegate(eq("delegateId"), anyInt(), isNotNull(), isNotNull(), eq(12), any()))
                .thenReturn(Mono.just(page1))
                .thenReturn(Mono.just(page2));

        when(pnDataVaultClient.getRecipientDenominationByInternalId(any()))
                .thenReturn(Flux.empty());

        when(pnInfoPaClient.getManyPa(any()))
                .thenReturn(Flux.empty());

        when(pnExtRegPrvtClient.getGroups(any(), Mockito.eq(false)))
                .thenReturn(Flux.empty());

        when(mandateEntityMandateDtoMapper.toDto(any()))
                .thenCallRealMethod();

        PageResultDto<MandateDto, String> resultDto = mandateSearchService.searchByDelegate(searchDto, lastEvaluatedKey)
                .block(D);
        assertNotNull(resultDto);
        assertEquals(1, resultDto.getPage().size());
        assertTrue(resultDto.isMore());
        assertFalse(resultDto.getNextPagesKey().isEmpty());
        verify(mandateEntityMandateDtoMapper).toDto(entity1);
    }

    @Test
    void testSearchByDelegateEmpty() {
        InputSearchMandateDto searchDto = InputSearchMandateDto.builder()
                .delegateId("delegateId")
                .size(1)
                .maxPageNumber(1)
                .build();

        PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
        lastEvaluatedKey.setExternalLastEvaluatedKey("20");
        lastEvaluatedKey.setInternalLastEvaluatedKey(Map.of("k", AttributeValue.builder().s("v").build()));

        Page<MandateEntity> page = Page.create(Collections.emptyList());
        when(mandateDao.searchByDelegate(anyString(), isNull(), any(), any(), anyInt(), any()))
                .thenReturn(Mono.just(page));

        when(pnExtRegPrvtClient.getGroups(any(), Mockito.eq(false)))
                .thenReturn(Flux.empty());

        PageResultDto<MandateDto, String> resultDto = mandateSearchService.searchByDelegate(searchDto, lastEvaluatedKey)
                .block(D);
        assertNotNull(resultDto);
        assertTrue(resultDto.getPage().isEmpty());
        assertFalse(resultDto.isMore());
        assertTrue(resultDto.getNextPagesKey().isEmpty());
        verifyNoInteractions(pnDataVaultClient);
        verifyNoInteractions(pnInfoPaClient);
        verifyNoInteractions(mandateEntityMandateDtoMapper);
    }
}
