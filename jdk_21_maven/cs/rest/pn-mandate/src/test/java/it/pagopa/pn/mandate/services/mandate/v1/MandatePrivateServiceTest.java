package it.pagopa.pn.mandate.services.mandate.v1;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.exceptions.PnForbiddenException;
import it.pagopa.pn.mandate.mapper.MandateEntityInternalMandateDtoMapper;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.middleware.db.MandateDao;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.utils.PgUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class MandatePrivateServiceTest extends AbstractTestConfiguration {

    @InjectMocks
    private MandatePrivateService mandatePrivateService;

    @Mock
    private MandateEntityInternalMandateDtoMapper mapper;

    @Mock
    private MandateDao mandateDao;

    @Mock
    PgUtils pgUtils;


    @Test
    void listMandatesByDelegate() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        List<MandateEntity> list = new ArrayList<>();
        list.add(mandateEntity);
        when(mandateDao.listMandatesByDelegate(any(), any()))
                .thenReturn(Flux.fromIterable(list));
        when(mapper.toDto(Mockito.same(mandateEntity))).thenReturn(new InternalMandateDto());

        //When
        List<InternalMandateDto> result = mandatePrivateService.listMandatesByDelegate(mandateEntity.getDelegate(), null, CxTypeAuthFleet.PF, null)
                .collectList()
                .block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void listMandatesByDelegateV2() {
        //Given
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder().build();
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        List<MandateEntity> list = new ArrayList<>();
        list.add(mandateEntity);
        when(mandateDao.listMandatesByDelegate(any(), any()))
                .thenReturn(Flux.fromIterable(list));
        when(mapper.toDto(Mockito.same(mandateEntity))).thenReturn(new InternalMandateDto());

        //When
        List<InternalMandateDto> result = mandatePrivateService.listMandatesByDelegateV2(inputSearchMandateDto)
                .collectList()
                .block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void listMandatesByDelegator() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        List<MandateEntity> list = new ArrayList<>();
        list.add(mandateEntity);
        when(mandateDao.listMandatesByDelegator(Mockito.same(mandateEntity.getDelegate()), any(), any(), any()))
                .thenReturn(Flux.fromIterable(list));
        when(mapper.toDto(Mockito.same(mandateEntity))).thenReturn(new InternalMandateDto());

        //When
        List<InternalMandateDto> result = mandatePrivateService.listMandatesByDelegator(mandateEntity.getDelegate(), null, CxTypeAuthFleet.PF, null, null, null)
                .collectList()
                .block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void listMandatesByDelegatorPG() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        Mono<List<InternalMandateDto>> objectMono = mandatePrivateService.listMandatesByDelegator(mandateEntity.getDelegate(), null, CxTypeAuthFleet.PG, new ArrayList<>(), "operatore", null)
                .collectList();

        //Then
        Assertions.assertThrows(PnForbiddenException.class, objectMono::block);
    }

    @Test
    void listMandatesByDelegators() {
        MandateEntity entity = MandateDaoIT.newMandate(true);
        entity.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        List<MandateEntity> list = new ArrayList<>();
        list.add(entity);
        when(mandateDao.listMandatesByDelegators(any()))
                .thenReturn(Flux.fromIterable(list));
        when(mapper.toDto(same(entity))).thenReturn(new InternalMandateDto());

        List<MandateByDelegatorRequestDto> requestDto = List.of(new MandateByDelegatorRequestDto());
        List<InternalMandateDto> result = mandatePrivateService.listMandatesByDelegators(DelegateType.PG, null, Flux.fromIterable(requestDto))
                .collectList()
                .block(Duration.ofMillis(3000));
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void listMandatesByDelegatorsFilter() {
        MandateEntity mandate1 = MandateDaoIT.newMandate(true);
        mandate1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandate1.setGroups(Set.of("RECLAMI"));
        MandateEntity mandate2 = MandateDaoIT.newMandate(true);
        mandate2.setGroups(Set.of("RECLAMI"));
        mandate2.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.PENDING));
        MandateEntity mandate3 = MandateDaoIT.newMandate(true);
        mandate3.setGroups(Set.of("PIPPO"));
        mandate3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        List<MandateEntity> list = List.of(mandate1, mandate2, mandate3);
        when(mandateDao.listMandatesByDelegators(any()))
                .thenReturn(Flux.fromIterable(list));
        when(mapper.toDto(same(mandate1))).thenReturn(new InternalMandateDto());

        List<MandateByDelegatorRequestDto> requestDto = List.of(new MandateByDelegatorRequestDto());
        List<InternalMandateDto> result = mandatePrivateService.listMandatesByDelegators(DelegateType.PG, List.of("RECLAMI"), Flux.fromIterable(requestDto))
                .collectList()
                .block(Duration.ofMillis(3000));
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}