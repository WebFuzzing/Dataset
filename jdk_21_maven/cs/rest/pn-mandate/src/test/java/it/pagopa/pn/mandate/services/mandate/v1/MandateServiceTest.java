package it.pagopa.pn.mandate.services.mandate.v1;

import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.ciechecker.model.ResultCieChecker;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationRequest;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationResponse;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.NISData;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import it.pagopa.pn.mandate.exceptions.*;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.DenominationDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.MandateDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.RecipientTypeDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.UserInfoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.UserInfoQrCodeDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.FilteredPaIdsResponseDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.PaSummaryDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.mandate.mapper.*;
import it.pagopa.pn.mandate.middleware.db.DelegateDao;
import it.pagopa.pn.mandate.middleware.db.MandateDao;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.middleware.db.entities.DelegateEntity;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.msclient.PnDataVaultClient;
import it.pagopa.pn.mandate.middleware.msclient.PnDeliveryClient;
import it.pagopa.pn.mandate.middleware.msclient.PnExtRegPrvtClient;
import it.pagopa.pn.mandate.middleware.msclient.PnInfoPaClient;
import it.pagopa.pn.mandate.model.PageResultDto;
import it.pagopa.pn.mandate.model.WorkFlowType;
import it.pagopa.pn.mandate.services.mandate.utils.MandateValidationUtils;
import it.pagopa.pn.mandate.utils.AarQrUtils;
import it.pagopa.pn.mandate.utils.Base64Validator;
import it.pagopa.pn.mandate.utils.DateUtils;
import it.pagopa.pn.mandate.utils.TypeSegregatorFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class MandateServiceTest extends AbstractTestConfiguration {

    private static final Duration D = Duration.ofMillis(3000);

    private static final String PG_ADMIN_ROLE = "admin";

    private MandateService mandateService;

    @Mock
    private MandateEntityMandateDtoMapper mapper;

    @Mock
    private ReverseMandateEntityMandateDtoMapper mapperB2b;

    @Mock
    private MandateDao mandateDao;

    @Mock
    private DelegateDao delegateDao;

    @Mock
    private UserEntityMandateCountsDtoMapper userEntityMandateCountsDtoMapper;

    @Mock
    private MandateEntityAppIoMandateDtoMapper mandateEntityAppIoMandateDtoMapper;

    @Mock
    private PnDeliveryClient pnDeliveryClient;

    @Mock
    private AarQrUtils aarQrUtils;

    @Mock
    private MandateEntityBuilderMapper mandateEntityBuilderMapper;

    @Mock
    private PnInfoPaClient pnInfoPaClient;

    @Mock
    private PnDataVaultClient pnDatavaultClient;

    @Mock
    private ValidateUtils validateUtils;


    @Mock
    private SqsService sqsService;

    @Mock
    private PnMandateConfig pnMandateConfig;

    @Mock
    private MandateSearchService mandateSearchService;

    @Mock
    private PnExtRegPrvtClient pnExtRegPrvtClient;

    @Mock
    private CieCheckerAdapter cieCheckerAdapter;

    @Mock
    private Base64Validator base64Validator;

    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
        MandateValidationUtils mandateValidationUtils = Mockito.spy(new MandateValidationUtils(validateUtils, pnExtRegPrvtClient));
        mandateService = new MandateService(mandateDao, delegateDao, mapper, mapperB2b, userEntityMandateCountsDtoMapper, pnInfoPaClient, pnDatavaultClient, sqsService, mandateValidationUtils, mandateSearchService, pnMandateConfig, pnDeliveryClient, mandateEntityAppIoMandateDtoMapper, aarQrUtils, mandateEntityBuilderMapper, cieCheckerAdapter, base64Validator);
    }

    @Test
    void updateMandatePGNotAuthorized() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        UpdateRequestDto updateRequestDto = new UpdateRequestDto();
        updateRequestDto.setGroups(new ArrayList<>());

        Mono<Void> objectMono = mandateService.updateMandate("cyId", CxTypeAuthFleet.PG, mandateEntity.getMandateId(), new ArrayList<>(), "OPERATOR", Mono.just(updateRequestDto));

        //When
        Assertions.assertThrows(PnForbiddenException.class, objectMono::block);
    }

    @Test
    void updateMandateWhereDelegateIsPerson() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        UpdateRequestDto updateRequestDto = new UpdateRequestDto();
        updateRequestDto.setGroups(new ArrayList<>());
        Tuple2<MandateEntity, MandateEntity> tuple2 = Tuples.of(mandateEntity, mandateEntity);
        Mono<Tuple2<MandateEntity, MandateEntity>> tuple2Mono = Mono.just(tuple2);
        when(mandateDao.updateMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(tuple2Mono);

        //When
        assertDoesNotThrow(() -> {
            mandateService.updateMandate("cyId", CxTypeAuthFleet.PG, mandateEntity.getMandateId(), new ArrayList<>(), "ADMIN", Mono.just(updateRequestDto)).block(D);

        });

        //Then
        verifyNoInteractions(sqsService);
    }

    @Test
    void updateMandateWhereDelegateIsNotPerson() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false);
        UpdateRequestDto updateRequestDto = new UpdateRequestDto();
        updateRequestDto.setGroups(new ArrayList<>());
        Tuple2<MandateEntity, MandateEntity> tuple2 = Tuples.of(mandateEntity, mandateEntity);
        Mono<Tuple2<MandateEntity, MandateEntity>> tuple2Mono = Mono.just(tuple2);
        when(mandateDao.updateMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(tuple2Mono);
        when(sqsService.sendToDelivery(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        //When
        assertDoesNotThrow(() -> {
            mandateService.updateMandate("cyId", CxTypeAuthFleet.PG, mandateEntity.getMandateId(), new ArrayList<>(), "ADMIN", Mono.just(updateRequestDto)).block(D);

        });
    }


    @Test
    void updateMandateWhereDelegateIsNotPerson_withgroups() {
        //Given
        String groupid = "123";
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false);
        UpdateRequestDto updateRequestDto = new UpdateRequestDto();
        updateRequestDto.setGroups(List.of(groupid));
        Tuple2<MandateEntity, MandateEntity> tuple2 = Tuples.of(mandateEntity, mandateEntity);
        Mono<Tuple2<MandateEntity, MandateEntity>> tuple2Mono = Mono.just(tuple2);
        when(mandateDao.updateMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(tuple2Mono);
        when(sqsService.sendToDelivery(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        List<PgGroupDto> groups = new ArrayList<>();
        PgGroupDto g1 = new PgGroupDto();
        g1.setId(groupid);
        g1.setName("gruppo1");
        groups.add(g1);
        PgGroupDto g2 = new PgGroupDto();
        g2.setId(groupid + "-2");
        g2.setName("gruppo2");
        groups.add(g2);

        when(pnExtRegPrvtClient.getGroups("cyId", true)).thenReturn(Flux.fromIterable(groups));

        //When
        assertDoesNotThrow(() -> {
            mandateService.updateMandate("cyId", CxTypeAuthFleet.PG, mandateEntity.getMandateId(), new ArrayList<>(), "ADMIN", Mono.just(updateRequestDto)).block(D);

        });
    }

    @Test
    void updateMandateWhereDelegateIsNotPerson_withgroups_fail() {
        //Given
        String groupid = "123";
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false);
        UpdateRequestDto updateRequestDto = new UpdateRequestDto();
        updateRequestDto.setGroups(List.of(groupid + "wrong"));
        Tuple2<MandateEntity, MandateEntity> tuple2 = Tuples.of(mandateEntity, mandateEntity);
        Mono<Tuple2<MandateEntity, MandateEntity>> tuple2Mono = Mono.just(tuple2);
        when(mandateDao.updateMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any())).thenReturn(tuple2Mono);
        when(sqsService.sendToDelivery(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());


        List<PgGroupDto> groups = new ArrayList<>();
        PgGroupDto g1 = new PgGroupDto();
        g1.setId(groupid);
        g1.setName("gruppo1");
        groups.add(g1);
        PgGroupDto g2 = new PgGroupDto();
        g2.setId(groupid + "-2");
        g2.setName("gruppo2");
        groups.add(g2);

        when(pnExtRegPrvtClient.getGroups("cyId", true)).thenReturn(Flux.fromIterable(groups));

        //When
        Mono<Void> mono = mandateService.updateMandate("cyId", CxTypeAuthFleet.PG, mandateEntity.getMandateId(), new ArrayList<>(), "ADMIN", Mono.just(updateRequestDto));
        assertThrows(PnInvalidGroupCodeException.class, () -> mono.block(D));

    }

    @Test
    void acceptMandatePGNotAuthorized() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(mandateEntity.getValidationcode());

        Mono<MandateEntity> objectMono = mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PG, new ArrayList<>(), "operator");

        //When
        Assertions.assertThrows(PnForbiddenException.class, objectMono::block);
    }

    @Test
    void acceptMandate() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(mandateEntity.getValidationcode());
        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));

        //When
        assertDoesNotThrow(() -> {
            mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PF, null, null).block(D);
        });

        //Then
        verifyNoInteractions(sqsService);
    }

    @Test
    void acceptMandatePG() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false); // PG
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(mandateEntity.getValidationcode());
        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_ACCEPTED)).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        //When
        assertDoesNotThrow(() -> {
            mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE).block(D);
        });

        //Then
        verify(sqsService).sendToDelivery(mandateEntity, EventType.MANDATE_ACCEPTED);
    }


    @Test
    void acceptMandatePG_withgroups() {
        //Given
        String groupid = "123";
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false); // PG
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(mandateEntity.getValidationcode());
        acceptRequestDto.setGroups(List.of(groupid));
        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_ACCEPTED)).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        List<PgGroupDto> groups = new ArrayList<>();
        PgGroupDto g1 = new PgGroupDto();
        g1.setId(groupid);
        g1.setName("gruppo1");
        groups.add(g1);
        PgGroupDto g2 = new PgGroupDto();
        g2.setId(groupid + "-2");
        g2.setName("gruppo2");
        groups.add(g2);

        when(pnExtRegPrvtClient.getGroups(mandateEntity.getDelegate(), true)).thenReturn(Flux.fromIterable(groups));

        //When
        assertDoesNotThrow(() -> {
            mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE).block(D);
        });

        //Then
        verify(sqsService).sendToDelivery(mandateEntity, EventType.MANDATE_ACCEPTED);
    }


    @Test
    void acceptMandatePG_withgroups_fail() {
        //Given
        String groupid = "123";
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false); // PG
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(mandateEntity.getValidationcode());
        acceptRequestDto.setGroups(List.of(groupid + "wrong"));
        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_ACCEPTED)).thenReturn(Mono.just(SendMessageResponse.builder().build()));

        List<PgGroupDto> groups = new ArrayList<>();
        PgGroupDto g1 = new PgGroupDto();
        g1.setId(groupid);
        g1.setName("gruppo1");
        groups.add(g1);
        PgGroupDto g2 = new PgGroupDto();
        g2.setId(groupid + "-2");
        g2.setName("gruppo2");
        groups.add(g2);

        when(pnExtRegPrvtClient.getGroups(mandateEntity.getDelegate(), true)).thenReturn(Flux.fromIterable(groups));

        //When
        Mono<MandateEntity> mono = mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE);
        assertThrows(PnInvalidGroupCodeException.class, () -> mono.block(D));
    }

    @Test
    void acceptMandateFailVercode() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(null);

        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        //When
        Mono<MandateEntity> mono = mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PF, null, null);
        assertThrows(PnInvalidVerificationCodeException.class, () -> mono.block(D));

        //Then
        // nothing, basta che non ci sia eccezione
    }


    @Test
    void acceptMandateFailWrongVercode() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode("44444");

        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenThrow(new PnInvalidVerificationCodeException());

        //When
        Mono<MandateEntity> mono = mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PF, null, null);
        assertThrows(PnInvalidVerificationCodeException.class, () -> mono.block(D));

        //Then
        // nothing, basta che non ci sia eccezione
    }


    @Test
    void acceptMandateFailWrongMandateId() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode("44444");

        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenThrow(new PnMandateNotFoundException());

        //When
        Mono<MandateEntity> mono = mandateService.acceptMandate(mandateEntity.getMandateId(), Mono.just(acceptRequestDto), mandateEntity.getDelegate(), CxTypeAuthFleet.PF, null, null);
        assertThrows(PnMandateNotFoundException.class, () -> mono.block(D));

        //Then
        // nothing, basta che non ci sia eccezione
    }


    @Test
    void acceptMandateFailMandateid() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        AcceptRequestDto acceptRequestDto = new AcceptRequestDto();
        acceptRequestDto.setVerificationCode(mandateEntity.getValidationcode());

        when(mandateDao.acceptMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.empty());

        //When
        Mono<AcceptRequestDto> monodto = Mono.just(acceptRequestDto);
        String delegate = mandateEntity.getDelegate();
        Mono<MandateEntity> mono = mandateService.acceptMandate(null, monodto, delegate, CxTypeAuthFleet.PF, null, null);
        Assertions.assertThrows(PnMandateNotFoundException.class, () -> mono.block(D));

        //Then
        // nothing, basta che non ci sia eccezione
    }

    @Test
    void countMandatesByDelegatePGNotAuthorized() {
        //Given
        DelegateEntity delegateEntity = new DelegateEntity();
        delegateEntity.setPendingcount(5);
        MandateCountsDto dto = new MandateCountsDto();
        dto.setValue(delegateEntity.getPendingcount());

        when(delegateDao.countMandates(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(delegateEntity));
        when(userEntityMandateCountsDtoMapper.toDto(Mockito.same(delegateEntity))).thenReturn(dto);

        //When
        Mono<MandateCountsDto> result = mandateService.countMandatesByDelegate(MandateDto.StatusEnum.PENDING.getValue(), "fake", CxTypeAuthFleet.PG, null, "operator");

        //Then
        Assertions.assertThrows(PnForbiddenException.class, result::block);
    }

    @Test
    void countMandatesByDelegate() {
        //Given
        DelegateEntity delegateEntity = new DelegateEntity();
        delegateEntity.setPendingcount(5);
        MandateCountsDto dto = new MandateCountsDto();
        dto.setValue(delegateEntity.getPendingcount());

        when(delegateDao.countMandates(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(delegateEntity));
        when(userEntityMandateCountsDtoMapper.toDto(Mockito.same(delegateEntity))).thenReturn(dto);

        //When
        MandateCountsDto result = mandateService.countMandatesByDelegate(MandateDto.StatusEnum.PENDING.getValue(), "fake", CxTypeAuthFleet.PF, null, null).block(D);

        //Then
        assertNotNull(result);
        assertEquals(delegateEntity.getPendingcount(), result.getValue());
    }

    @Test
    void countMandatesByDelegateInvalid1() {
        //Given
        DelegateEntity delegateEntity = new DelegateEntity();
        delegateEntity.setPendingcount(5);
        MandateCountsDto dto = new MandateCountsDto();
        dto.setValue(delegateEntity.getPendingcount());

        when(delegateDao.countMandates(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(delegateEntity));
        when(userEntityMandateCountsDtoMapper.toDto(Mockito.same(delegateEntity))).thenReturn(dto);

        //When
        Mono<MandateCountsDto> mono = mandateService.countMandatesByDelegate(null, "fake", CxTypeAuthFleet.PF, null, null);
        Assertions.assertThrows(PnUnsupportedFilterException.class, mono::block);

    }

    @Test
    void countMandatesByDelegateInvalid2() {
        //Given
        DelegateEntity delegateEntity = new DelegateEntity();
        delegateEntity.setPendingcount(5);
        MandateCountsDto dto = new MandateCountsDto();
        dto.setValue(delegateEntity.getPendingcount());

        when(delegateDao.countMandates(Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(delegateEntity));
        when(userEntityMandateCountsDtoMapper.toDto(Mockito.same(delegateEntity))).thenReturn(dto);

        //When
        String status = MandateDto.StatusEnum.ACTIVE.getValue();
        Mono<MandateCountsDto> mono = mandateService.countMandatesByDelegate(status, "fake", CxTypeAuthFleet.PF, null, null);
        Assertions.assertThrows(PnUnsupportedFilterException.class, mono::block);

    }

    @Test
    void createMandatePGNotAuthorized() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        //When
        Mono<MandateDto> result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PG, "WEB", null, "operator");

        //Then
        Assertions.assertThrows(PnForbiddenException.class, result::block);
    }

    @Test
    void createMandate() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestName("mario");
        denominationDtoDto.setDestSurname("rossi");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);


        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(true), Mockito.eq(false))).thenReturn(true);

        //When
        MandateDto result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null).block(D);

        //Then
        assertNotNull(result);
        assertNotNull(result.getMandateId());
        assertNotNull(result.getStatus());
        assertEquals(mandateDto.getVerificationCode(), result.getVerificationCode());
    }

    @Test
    void createReverseMandate() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();
        mandateDtoRequest.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRequest.setDelegator(new UserDto());
        mandateDtoRequest.getDelegator().setFirstName("mario");
        mandateDtoRequest.getDelegator().setLastName("rossi");
        mandateDtoRequest.getDelegator().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRequest.getDelegator().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestName("mario");
        denominationDtoDto.setDestSurname("rossi");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);

        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        BaseRecipientDtoDto baseRecipientDtoDto = new BaseRecipientDtoDto();
        baseRecipientDtoDto.setDenomination("test");
        when(pnDatavaultClient.getRecipientDenominationByInternalId(anyList())).thenReturn(Flux.just(baseRecipientDtoDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegator()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(true), Mockito.eq(false))).thenReturn(true);
        when(mapperB2b.toEntity(any())).thenReturn(entity);
        //When
        String result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null).block(D);

        //Then
        assertNotNull(result);

    }

    @Test
    void createReverseMandateDelegatorNull() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();

        Mono<String> result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null);
        assertThrows(PnInvalidInputException.class, () -> result.block(D));
    }

    @Test
    void createReverseMandateFiscalCodeNull() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();
        mandateDtoRequest.setDelegator(new UserDto());

        Mono<String> result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null);
        assertThrows(PnInvalidInputException.class, () -> result.block(D));
    }

    @Test
    void createReverseMandatePersonNull() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();
        mandateDtoRequest.setDelegator(new UserDto());
        mandateDtoRequest.getDelegator().setFiscalCode("LVLDAA85T50G702B");

        Mono<String> result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null);
        assertThrows(PnInvalidInputException.class, () -> result.block(D));
    }

    @Test
    void createReverseMandateInvalidDelegator() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();
        mandateDtoRequest.setDelegator(new UserDto());
        mandateDtoRequest.getDelegator().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRequest.getDelegator().setPerson(true);

        Mono<String> result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null);
        assertThrows(PnInvalidInputException.class, () -> result.block(D));
    }

    @Test
    void crateReverseMandateInvalidFiscalCode() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();
        mandateDtoRequest.setDelegator(new UserDto());
        mandateDtoRequest.getDelegator().setFirstName("mario");
        mandateDtoRequest.getDelegator().setLastName("rossi");
        mandateDtoRequest.getDelegator().setFiscalCode("FAKEFAKEFAKE");
        mandateDtoRequest.getDelegator().setPerson(true);

        Mono<String> result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null);
        assertThrows(PnInvalidInputException.class, () -> result.block(D));
    }

    @Test
    void createReverseMandateDateToNull() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        final MandateDtoRequest mandateDtoRequest = new MandateDtoRequest();
        mandateDtoRequest.setDelegator(new UserDto());
        mandateDtoRequest.getDelegator().setFirstName("mario");
        mandateDtoRequest.getDelegator().setLastName("rossi");
        mandateDtoRequest.getDelegator().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRequest.getDelegator().setPerson(true);

        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(true), Mockito.eq(false))).thenReturn(true);
        Mono<String> result = mandateService.createReverseMandate(mandateDtoRequest, null, entity.getDelegate(), CxTypeAuthFleet.PF, "APP", null, null);
        assertThrows(PnInvalidInputException.class, () -> result.block(D));
    }

    @Test
    void createMandate_DelegateIsPG() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        entity.setDelegateisperson(false);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setCompanyName("mario srl");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setCompanyName("mario srl");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestBusinessName("mario srl");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);

        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(false), Mockito.eq(false))).thenReturn(true);

        //When
        MandateDto result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null).block(D);

        //Then
        assertNotNull(result);
        assertNotNull(result.getMandateId());
        assertNotNull(result.getStatus());
        assertEquals(mandateDto.getVerificationCode(), result.getVerificationCode());
    }

    @Test
    void createMandate_DelegatorIsPG() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestName("mario");
        denominationDtoDto.setDestSurname("rossi");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);

        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(true), Mockito.eq(false))).thenReturn(true);

        //When
        MandateDto result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PG, "WEB", null, "admin").block(D);

        //Then
        assertNotNull(result);
        assertNotNull(result.getMandateId());
        assertNotNull(result.getStatus());
        assertEquals(mandateDto.getVerificationCode(), result.getVerificationCode());
    }

    @Test
    void createMandate_PG_piva() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        entity.setDelegateisperson(false);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setCompanyName("mario srl");
        mandateDto.getDelegate().setFiscalCode("15376371009");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setCompanyName("mario srl");
        mandateDtoRes.getDelegate().setFiscalCode("15376371009");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestBusinessName("mario srl");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);


        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(false), Mockito.eq(false))).thenReturn(true);

        //When
        MandateDto result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null).block(D);

        //Then
        assertNotNull(result);
        assertNotNull(result.getMandateId());
        assertNotNull(result.getStatus());
        assertEquals(mandateDto.getVerificationCode(), result.getVerificationCode());
    }

    @Test
    void createMandateWithList() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        OrganizationIdDto organizationIdDto = new OrganizationIdDto();
        organizationIdDto.setUniqueIdentifier("abcd");
        mandateDto.getVisibilityIds().add(organizationIdDto);
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.getVisibilityIds().add(organizationIdDto);
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestName("mario");
        denominationDtoDto.setDestSurname("rossi");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);

        PaSummaryDto paSummaryDto = new PaSummaryDto();
        paSummaryDto.setId(organizationIdDto.getUniqueIdentifier());
        paSummaryDto.setName("nome");


        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(paSummaryDto)));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(false))).thenReturn(true);
        when(pnExtRegPrvtClient.checkAooUoV2Ids(Mockito.anyList())).thenReturn(Mono.just(new FilteredPaIdsResponseDto()));

        //When
        MandateDto result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null).block(D);

        //Then
        assertNotNull(result);
        assertNotNull(result.getMandateId());
        assertNotNull(result.getStatus());
        assertEquals(mandateDto.getVerificationCode(), result.getVerificationCode());
        assertEquals(paSummaryDto.getName(), result.getVisibilityIds().get(0).getName());
    }


    @Test
    void createMandateWithInvalidVisibilityId() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        OrganizationIdDto organizationIdDto = new OrganizationIdDto();
        organizationIdDto.setUniqueIdentifier("abcd");
        mandateDto.getVisibilityIds().add(organizationIdDto);
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.getVisibilityIds().add(organizationIdDto);
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestName("mario");
        denominationDtoDto.setDestSurname("rossi");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);

        PaSummaryDto paSummaryDto = new PaSummaryDto();
        paSummaryDto.setId(organizationIdDto.getUniqueIdentifier());
        paSummaryDto.setName("nome");


        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(paSummaryDto)));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.anyBoolean(), Mockito.eq(false))).thenReturn(true);

        List<String> ids = List.of("id1","id2");
        FilteredPaIdsResponseDto filteredPaIdsResponseDto= new FilteredPaIdsResponseDto();
        ids.forEach(filteredPaIdsResponseDto::addIdsItem);
        when(pnExtRegPrvtClient.checkAooUoV2Ids(Mockito.anyList())).thenReturn(Mono.just(filteredPaIdsResponseDto));


        //When
        Mono<MandateDto> mono = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        //Then
        assertThrows(PnInvalidVisibilityIdException.class, () -> mono.block(D));

    }

    @Test
    void createMandateWithListMany() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MandateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        OrganizationIdDto organizationIdDto = new OrganizationIdDto();
        organizationIdDto.setUniqueIdentifier("abcd");
        mandateDto.getVisibilityIds().add(organizationIdDto);
        OrganizationIdDto organizationIdDto1 = new OrganizationIdDto();
        organizationIdDto1.setUniqueIdentifier("fghi");
        mandateDto.getVisibilityIds().add(organizationIdDto1);

        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.getVisibilityIds().add(organizationIdDto);
        mandateDtoRes.getVisibilityIds().add(organizationIdDto1);
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateDtoDto> resgetmandatesbyid = new ArrayList<>();
        MandateDtoDto mandateDtoDto = new MandateDtoDto();
        mandateDtoDto.mandateId(entity.getMandateId());
        DenominationDtoDto denominationDtoDto = new DenominationDtoDto();
        denominationDtoDto.setDestName("mario");
        denominationDtoDto.setDestSurname("rossi");
        mandateDtoDto.setInfo(denominationDtoDto);
        resgetmandatesbyid.add(mandateDtoDto);

        PaSummaryDto paSummaryDto = new PaSummaryDto();
        paSummaryDto.setId(organizationIdDto.getUniqueIdentifier());
        paSummaryDto.setName("nome");
        PaSummaryDto paSummaryDto1 = new PaSummaryDto();
        paSummaryDto1.setId(organizationIdDto1.getUniqueIdentifier());
        paSummaryDto1.setName("nome2");


        when(mandateDao.createMandate(Mockito.any(), Mockito.any())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegate()));
        when(pnDatavaultClient.updateMandateById(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Mono.just("OK"));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.fromIterable(resgetmandatesbyid));
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(paSummaryDto, paSummaryDto1)));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(true), Mockito.eq(false))).thenReturn(true);
        when(pnExtRegPrvtClient.checkAooUoV2Ids(Mockito.anyList())).thenReturn(Mono.just(new FilteredPaIdsResponseDto()));

        //When
        MandateDto result = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegator(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null).block(D);

        //Then
        assertNotNull(result);
        assertNotNull(result.getMandateId());
        assertNotNull(result.getStatus());
        assertEquals(mandateDto.getVerificationCode(), result.getVerificationCode());
        assertEquals(paSummaryDto.getName(), result.getVisibilityIds().get(0).getName());
        assertEquals(paSummaryDto1.getName(), result.getVisibilityIds().get(1).getName());
    }

    @Test
    void createMandateFailFiscalcode() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("FAKEFAKEFAKE");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        //When
        Mono<MandateDto> monodto = Mono.just(mandateDto);
        String delegate = entity.getDelegate();
        Mono<MandateDto> mono = mandateService.createMandate(monodto, delegate, entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnInvalidInputException.class, () -> mono.block(D));

        //Then
        //nothing
    }

    @Test
    void createMandateFailFiscalcode_PG() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setCompanyName("mario srl");
        mandateDto.getDelegate().setFiscalCode("FAKEFAKEFAKE");
        mandateDto.getDelegate().setPerson(false);

        //When
        Mono<MandateDto> monodto = Mono.just(mandateDto);
        String delegate = entity.getDelegate();
        Mono<MandateDto> mono = mandateService.createMandate(monodto, delegate, entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnInvalidInputException.class, () -> mono.block(D));

        //Then
        //nothing
    }

    @Test
    void createMandateFailFiscalcodeNull() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode(null);
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        //When
        Mono<MandateDto> monodto = Mono.just(mandateDto);
        String delegate = entity.getDelegate();
        Mono<MandateDto> mono = mandateService.createMandate(monodto, delegate, entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnInvalidInputException.class, () -> mono.block(D));

        //Then
        //nothing
    }

    @Test
    void createMandateFailVercodeNull() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(null);
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        //When
        Mono<MandateDto> monodto = Mono.just(mandateDto);
        String delegate = entity.getDelegate();
        Mono<MandateDto> mono = mandateService.createMandate(monodto, delegate, entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnInvalidInputException.class, () -> mono.block(D));

        //Then
        //nothing
    }

    @Test
    void createMandateFailVercodeInvalidformat() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode("123456");
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        //When
        Mono<MandateDto> monodto = Mono.just(mandateDto);
        String delegate = entity.getDelegate();
        Mono<MandateDto> mono = mandateService.createMandate(monodto, delegate, entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnInvalidInputException.class, () -> mono.block(D));

        //Then
        //nothing
    }


    @Test
    void createMandateFailNoValidTo() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(null);
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        //When
        Mono<MandateDto> mono = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegate(), entity.getDelegatorUid(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnInvalidInputException.class, () -> mono.block(D));

        //Then
        //nothing
    }


    @Test
    void createMandateFailSameUserDelegateDelegator() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);

        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(null);
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.setDateto(Instant.now().toString());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        when(pnDatavaultClient.ensureRecipientByExternalId(Mockito.anyBoolean(), Mockito.anyString())).thenReturn(Mono.just(entity.getDelegator()));
        when(validateUtils.validate(Mockito.anyString(), Mockito.eq(true), Mockito.eq(false))).thenReturn(true);

        //When
        Mono<MandateDto> mono = mandateService.createMandate(Mono.just(mandateDto), entity.getDelegatorUid(), entity.getDelegator(), CxTypeAuthFleet.PF, "WEB", null, null);
        assertThrows(PnMandateByHimselfException.class, () -> mono.block(D));

        //Then
        //nothing
    }

    @Test
    void listMandatesByDelegate() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);

        when(mandateDao.listMandatesByDelegate(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.empty());
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);

        //When
        List<MandateDto> result = mandateService.listMandatesByDelegate(null, entity.getDelegate(), CxTypeAuthFleet.PF, null, null).collectList().block(D);

        //Then
        assertNotNull(result);

        assertEquals(1, result.size());
    }


    @Test
    void listMandatesByDelegate_pgnotallowed() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);

        when(mandateDao.listMandatesByDelegate(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.empty());
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);

        //When
        Mono<List<MandateDto>> mono = mandateService.listMandatesByDelegate(null, entity.getDelegate(), CxTypeAuthFleet.PG, null, "ADMIN").collectList();
        Assertions.assertThrows(PnForbiddenException.class, () -> mono.block(D));

        //Then
        Mockito.verify(mandateDao, Mockito.never()).listMandatesByDelegate(Mockito.any(), Mockito.any());

    }

    @Test
    void listMandatesByDelegateWithPas() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        OrganizationIdDto organizationIdDto = new OrganizationIdDto();
        organizationIdDto.setUniqueIdentifier("abcd");
        mandateDto.getVisibilityIds().add(organizationIdDto);
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.getVisibilityIds().add(organizationIdDto);
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);

        when(mandateDao.listMandatesByDelegate(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.empty());
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);

        //When
        List<MandateDto> result = mandateService.listMandatesByDelegate(null, entity.getDelegate(), CxTypeAuthFleet.PF, null, null).collectList().block(D);

        //Then
        assertNotNull(result);

        assertEquals(1, result.size());
    }

    @Test
    void listMandatesByDelegateWithDenominations() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        OrganizationIdDto organizationIdDto = new OrganizationIdDto();
        organizationIdDto.setUniqueIdentifier("abcd");
        mandateDto.getVisibilityIds().add(organizationIdDto);
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.getVisibilityIds().add(organizationIdDto);
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());
        mandateDtoRes.setDelegator(new UserDto());


        List<MandateEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);

        List<BaseRecipientDtoDto> denominations = new ArrayList<>();
        denominations.add(new BaseRecipientDtoDto());
        denominations.get(0).setInternalId(entity.getDelegator());
        denominations.get(0).setDenomination("test user");
        denominations.get(0).setRecipientType(RecipientTypeDto.PF);
        denominations.get(0).setTaxId("TAXIDDELEGATOR");

        when(mandateDao.listMandatesByDelegate(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.fromIterable(denominations));
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.empty());
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);

        //When
        List<MandateDto> result = mandateService.listMandatesByDelegate(null, entity.getDelegate(), CxTypeAuthFleet.PF, null, null).collectList().block(D);

        //Then
        assertNotNull(result);

        assertEquals(1, result.size());
    }

    @Test
    void listMandatesByDelegateInvalid1() {
        //Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);

        when(mandateDao.listMandatesByDelegate(Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.empty());
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);

        //When
        String delegate = entity.getDelegate();
        Assertions.assertThrows(PnUnsupportedFilterException.class, () -> mandateService.listMandatesByDelegate("INVALID", delegate, CxTypeAuthFleet.PF, null, null));
    }

    @Test
    void listMandatesByDelegator() {
//Given
        MandateEntity entity = MandateDaoIT.newMandate(true);
        // MAndateDto come proviene da FE quindi senza alcune info
        final MandateDto mandateDto = new MandateDto();
        mandateDto.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDto.setVerificationCode(entity.getValidationcode());
        mandateDto.setVisibilityIds(new ArrayList<>());
        mandateDto.setDelegate(new UserDto());
        mandateDto.getDelegate().setFirstName("mario");
        mandateDto.getDelegate().setLastName("rossi");
        mandateDto.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDto.getDelegate().setPerson(entity.getDelegateisperson());

        final MandateDto mandateDtoRes = new MandateDto();
        mandateDtoRes.setMandateId(entity.getMandateId());
        mandateDtoRes.setDatefrom(DateUtils.formatDate(entity.getValidfrom()));
        mandateDtoRes.setStatus(MandateDto.StatusEnum.PENDING);
        mandateDtoRes.setDateto(DateUtils.formatDate(entity.getValidto()));
        mandateDtoRes.setVerificationCode(entity.getValidationcode());
        mandateDtoRes.setVisibilityIds(new ArrayList<>());
        mandateDtoRes.setDelegate(new UserDto());
        mandateDtoRes.getDelegate().setFirstName("mario");
        mandateDtoRes.getDelegate().setLastName("rossi");
        mandateDtoRes.getDelegate().setFiscalCode("LVLDAA85T50G702B");
        mandateDtoRes.getDelegate().setPerson(entity.getDelegateisperson());

        List<MandateEntity> listFromDb = new ArrayList<>();
        listFromDb.add(entity);

        when(mandateDao.listMandatesByDelegator(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Flux.fromIterable(listFromDb));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(Mockito.any())).thenReturn(Flux.empty());
        when(pnDatavaultClient.getMandatesByIds(Mockito.any())).thenReturn(Flux.empty());
        when(pnInfoPaClient.getManyPa(Mockito.anyList())).thenReturn(Flux.fromIterable(List.of(new PaSummaryDto())));
        when(mapper.toEntity(Mockito.any())).thenReturn(entity);
        when(mapper.toDto(Mockito.any())).thenReturn(mandateDtoRes);

        //When
        List<MandateDto> result = mandateService.listMandatesByDelegator(entity.getDelegate(), CxTypeAuthFleet.PF, null, null).collectList().block(D);

        //Then
        assertNotNull(result);

        assertEquals(1, result.size());
    }

    @Test
    void rejectMandate() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.rejectMandate(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.just("").then());

        //When
        assertDoesNotThrow(() -> {
            mandateService.rejectMandate(mandateEntity.getMandateId(), mandateEntity.getDelegate(), CxTypeAuthFleet.PF, null, null).block(D);
        });

        //Then
        verifyNoInteractions(sqsService);
    }

    @Test
    void rejectMandatePGNotAuthorized() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.rejectMandate(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.empty());

        Mono<Void> resp = mandateService.rejectMandate(mandateEntity.getMandateId(), mandateEntity.getDelegate(), CxTypeAuthFleet.PG, "operator", new ArrayList<>());
        //When
        Assertions.assertThrows(PnForbiddenException.class, () -> resp.block(D));
    }

    @Test
    void rejectMandatePG() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false); // PG

        when(sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_REJECTED)).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        when(mandateDao.rejectMandate(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(mandateEntity.getMandateId())).thenReturn(Mono.just("").then());

        //When
        assertDoesNotThrow(() -> {
            mandateService.rejectMandate(mandateEntity.getMandateId(), mandateEntity.getDelegate(), CxTypeAuthFleet.PG, PG_ADMIN_ROLE, null).block(D);
        });

        //Then
        verify(sqsService).sendToDelivery(mandateEntity, EventType.MANDATE_REJECTED);
    }

    @Test
    void rejectMandateFailMandateId() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.rejectMandate(Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.empty());
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.empty());

        //When
        String delegate = mandateEntity.getDelegate();
        Mono<Void> mono = mandateService.rejectMandate(null, delegate, CxTypeAuthFleet.PF, null, null);
        assertThrows(PnMandateNotFoundException.class, mono::block);

        //Then
        // nothing, basta che non ci sia eccezione
    }

    @Test
    void revokeMandate() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.revokeMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.just("").then());

        //When
        assertDoesNotThrow(() -> {
            mandateService.revokeMandate(mandateEntity.getMandateId(), mandateEntity.getDelegator(), CxTypeAuthFleet.PF, null, null).block(D);
        });

        //Then
        verifyNoInteractions(sqsService);
    }

    @Test
    void revokeMandatePGNotAuthorized() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.revokeMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.empty());

        Mono<Object> resp = mandateService.revokeMandate(mandateEntity.getMandateId(), mandateEntity.getDelegator(), CxTypeAuthFleet.PG, null, null);
        //When
        assertThrows(PnForbiddenException.class, () -> resp.block(D));
    }

    @Test
    void revokeMandatePG() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false); // PG

        when(sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_REVOKED)).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        when(mandateDao.revokeMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.just("").then());

        //When
        assertDoesNotThrow(() -> {
            mandateService.revokeMandate(mandateEntity.getMandateId(), mandateEntity.getDelegator(), CxTypeAuthFleet.PG, PG_ADMIN_ROLE, null).block(D);
        });

        //Then
        verify(sqsService).sendToDelivery(mandateEntity, EventType.MANDATE_REVOKED);
    }

    @Test
    void revokeMandateFailMandateId() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.revokeMandate(Mockito.anyString(), Mockito.anyString(), Mockito.any(), Mockito.any())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.empty());

        //When
        String delegator = mandateEntity.getDelegator();
        Mono<Object> mono = mandateService.revokeMandate(null, delegator, CxTypeAuthFleet.PF, null, null);
        assertThrows(PnMandateNotFoundException.class, mono::block);

        //Then
        // nothing, basta che non ci sia eccezione
    }

    @Test
    void expireMandate() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.expireMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.just("").then());

        //When
        assertDoesNotThrow(() -> {
            mandateService.expireMandate(mandateEntity.getMandateId(), mandateEntity.getDelegatorUid(), mandateEntity.getDelegatorisperson() ? "PF" : "PG", mandateEntity.getDelegator()).block(D);
        });

        //Then
        verifyNoInteractions(sqsService);
    }

    @Test
    void testExpireMandatePG() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);
        mandateEntity.setDelegateisperson(false); // PG

        when(sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_EXPIRED)).thenReturn(Mono.just(SendMessageResponse.builder().build()));
        when(mandateDao.expireMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.just("").then());

        //When
        assertDoesNotThrow(() -> {
            mandateService.expireMandate(mandateEntity.getMandateId(), mandateEntity.getDelegatorUid(), mandateEntity.getDelegateisperson() ? "PF" : "PG", mandateEntity.getDelegator()).block(D);
        });

        //Then
        verify(sqsService).sendToDelivery(mandateEntity, EventType.MANDATE_EXPIRED);
    }

    @Test
    void expireMandateFailMandateId() {
        //Given
        MandateEntity mandateEntity = MandateDaoIT.newMandate(true);

        when(mandateDao.expireMandate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Mono.just(mandateEntity));
        when(pnDatavaultClient.deleteMandateById(Mockito.any())).thenReturn(Mono.empty());

        //When
        String delegator = mandateEntity.getDelegator();
        Assertions.assertThrows(PnMandateNotFoundException.class, () -> mandateService.expireMandate(null, "", "", delegator));

        //Then
        // nothing, basta che non ci sia eccezione
    }

    @Test
    void searchByDelegateWithCF() {
        //Given
        List<MandateDto> page = List.of(new MandateDto());
        List<String> lek = List.of("");
        PageResultDto<MandateDto, String> pageResultDto = PageResultDto.<MandateDto, String>builder().page(page).more(true).nextPagesKey(lek).build();

        when(pnMandateConfig.getMaxPageSize()).thenReturn(1);
        when(mandateSearchService.searchByDelegate(any(), any())).thenReturn(Mono.just(pageResultDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(anyBoolean(), any())).thenReturn(Mono.just(UUID.randomUUID().toString()));

        //When
        SearchMandateRequestDto requestDto = new SearchMandateRequestDto();
        requestDto.setTaxId("BBBCCC87D22E111A");
        SearchMandateResponseDto responseDto = mandateService.searchByDelegate(Mono.just(requestDto), 1, null, "cx-id", CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE).block(D);

        //Then
        assertNotNull(responseDto);
        assertTrue(responseDto.getMoreResult());
        assertSame(page, responseDto.getResultsPage());
        assertSame(lek, responseDto.getNextPagesKey());
    }

    @Test
    void searchByDelegateWithNotFoundCF() {
        //Given
        List<MandateDto> page = List.of(new MandateDto());
        List<String> lek = List.of("");
        PageResultDto<MandateDto, String> pageResultDto = PageResultDto.<MandateDto, String>builder().page(page).more(true).nextPagesKey(lek).build();

        when(pnMandateConfig.getMaxPageSize()).thenReturn(1);
        when(mandateSearchService.searchByDelegate(any(), any())).thenReturn(Mono.just(pageResultDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(anyBoolean(), any())).thenReturn(Mono.empty());

        //When
        SearchMandateRequestDto requestDto = new SearchMandateRequestDto();
        requestDto.setTaxId("BBBCCC87D22E111A");
        SearchMandateResponseDto responseDto = mandateService.searchByDelegate(Mono.just(requestDto), 1, null, "cx-id", CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE).block(D);

        //Then
        assertNotNull(responseDto);
        assertFalse(responseDto.getMoreResult());
        assertSame(0, responseDto.getResultsPage().size());
        assertSame(0, responseDto.getNextPagesKey().size());
    }

    @Test
    void searchByDelegateWithVatNumber() {
        //Given
        List<MandateDto> page = List.of(new MandateDto());
        List<String> lek = List.of("");
        PageResultDto<MandateDto, String> pageResultDto = PageResultDto.<MandateDto, String>builder().page(page).more(true).nextPagesKey(lek).build();

        when(pnMandateConfig.getMaxPageSize()).thenReturn(1);
        when(mandateSearchService.searchByDelegate(any(), any())).thenReturn(Mono.just(pageResultDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(anyBoolean(), any())).thenReturn(Mono.just(UUID.randomUUID().toString()));

        //When
        SearchMandateRequestDto requestDto = new SearchMandateRequestDto();
        requestDto.setTaxId("12345678912");
        SearchMandateResponseDto responseDto = mandateService.searchByDelegate(Mono.just(requestDto), 1, null, "cx-id", CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE).block(D);

        //Then
        assertNotNull(responseDto);
        assertTrue(responseDto.getMoreResult());
        assertSame(page, responseDto.getResultsPage());
        assertSame(lek, responseDto.getNextPagesKey());
    }

    @Test
    void searchByDelegateWithoutTaxId() {
        //Given
        List<MandateDto> page = List.of(new MandateDto());
        List<String> lek = List.of("");
        PageResultDto<MandateDto, String> pageResultDto = PageResultDto.<MandateDto, String>builder().page(page).more(true).nextPagesKey(lek).build();

        when(pnMandateConfig.getMaxPageSize()).thenReturn(1);
        when(mandateSearchService.searchByDelegate(any(), any())).thenReturn(Mono.just(pageResultDto));

        //When
        SearchMandateRequestDto requestDto = new SearchMandateRequestDto();
        SearchMandateResponseDto responseDto = mandateService.searchByDelegate(Mono.just(requestDto), 1, null, "cx-id", CxTypeAuthFleet.PG, null, PG_ADMIN_ROLE).block(D);

        //Then
        assertNotNull(responseDto);
        assertTrue(responseDto.getMoreResult());
        assertSame(page, responseDto.getResultsPage());
        assertSame(lek, responseDto.getNextPagesKey());
    }

    @Test
    void createMandateAppIo_success() {
        // Given
        String xPagopaPnCxId = "PF-f271e4bf-0d69-4ed6-a39f-4ef2delegate";
        String lollipopUserName = "John";
        String lollipopUserFamilyName = "Doe";
        String iun = "QDYU-PUAD-QMQA-202305-G-3";
        it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet xPagopaPnCxType = it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet.PF;
        MandateCreationRequest request = new MandateCreationRequest();
        request.setAarQrCodeValue("qrCodeValue");
        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setTaxId("TAXID123");
        userInfoDto.setDenomination("Danilo Longobaldi");
        String decodedQr = "decodedQr";
        UserInfoQrCodeDto userInfoQrCodeDto = new UserInfoQrCodeDto();
        userInfoQrCodeDto.setIun(iun);
        userInfoQrCodeDto.setRecipientInfo(userInfoDto);
        String delegatorInternalUserId = "PF-f271e4bf-0d69-4ed6-a39f-4efdelegator";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId("mandateId");
        entity.setDelegatorUid("f271e4bf-0d69-4ed6-a39f-4efdelegator");
        entity.setDelegator("PF-f271e4bf-0d69-4ed6-a39f-4efdelegator");
        entity.setDelegate("PF-f271e4bf-0d69-4ed6-a39f-4ef2delegate");
        entity.setDelegatorisperson(true);
        entity.setDelegateisperson(true);
        entity.setValidfrom(ZonedDateTime.of(LocalDateTime.of(2021, Month.DECEMBER, 14, 0, 0), ZoneId.of("Europe/Rome")).toInstant());
        entity.setValidto(Instant.now().plus(Duration.ofDays(3)));
        entity.setValidationcode("12345");
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setIuns(Set.of("QDYU-PUAD-QMQA-202305-G-3"));
        entity.setCreated(Instant.now());
        MandateCreationResponse response = new MandateCreationResponse();

        // Mock
        when(aarQrUtils.extractQrToken("qrCodeValue")).thenReturn(decodedQr);
        when(pnDeliveryClient.decodeAarQrCode(decodedQr)).thenReturn(Mono.just(userInfoQrCodeDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(true, "TAXID123")).thenReturn(Mono.just(delegatorInternalUserId));
        when(pnDatavaultClient.updateMandateById(any(), any(), any(), any())).thenReturn(Mono.just("OK"));
        when(mandateEntityBuilderMapper.buildMandateEntity(any(), any(), any(), any())).thenReturn(entity);
        when(mandateDao.createMandate(entity, TypeSegregatorFilter.CIE)).thenReturn(Mono.just(entity));
        when(mandateEntityAppIoMandateDtoMapper.toDto(entity)).thenReturn(response);

        // When
        MandateCreationResponse result = mandateService.createMandateAppIo(xPagopaPnCxId, lollipopUserName, lollipopUserFamilyName, xPagopaPnCxType, Mono.just(request)).block(D);

        // Then
        assertNotNull(result);
        verify(aarQrUtils).extractQrToken("qrCodeValue");
        verify(pnDeliveryClient).decodeAarQrCode(decodedQr);
        verify(pnDatavaultClient).ensureRecipientByExternalId(true, "TAXID123");
        verify(mandateEntityBuilderMapper).buildMandateEntity(any(), any(), any(), any());
        verify(pnDatavaultClient).updateMandateById(any(), any(), any(), any());
        verify(mandateDao).createMandate(entity, TypeSegregatorFilter.CIE);
        verify(mandateEntityAppIoMandateDtoMapper).toDto(entity);
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDelegateData")
    void createMandateAppIo_shouldReturn500_whenDelegateNameOrFamilyNameIsMissingOrEmpty(String lollipopUserName, String lollipopUserFamilyName) {
        // Given
        MandateCreationRequest request = new MandateCreationRequest();
        request.setAarQrCodeValue("qrCodeValue");

        Mono<MandateCreationResponse> result = mandateService.createMandateAppIo("cxId", lollipopUserName, lollipopUserFamilyName, it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet.PF, Mono.just(request));

        StepVerifier.create(result).expectError(PnInternalException.class).verify();
    }

    private static Stream<Arguments> provideInvalidDelegateData() {
        return Stream.of(Arguments.of("", "Doe"), Arguments.of(" ", "Doe"), Arguments.of(null, "Doe"), Arguments.of("John", ""), Arguments.of("John", " "), Arguments.of("John", null));
    }

    @Test
    void createMandateAppIo_shouldNotAllowRequestForHimself() {
        // Given
        MandateCreationRequest request = new MandateCreationRequest();
        request.setAarQrCodeValue("qrCodeValue");

        String decodedQr = "decodedQr";
        UserInfoQrCodeDto userInfoQrCodeDto = new UserInfoQrCodeDto();
        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setTaxId("TAXID123");
        userInfoQrCodeDto.setRecipientInfo(userInfoDto);
        String delegatorInternalUserId = "internalUserId";
        String delegateInternalUserId = "internalUserId";

        when(aarQrUtils.extractQrToken("qrCodeValue")).thenReturn(decodedQr);
        when(pnDeliveryClient.decodeAarQrCode(decodedQr)).thenReturn(Mono.just(userInfoQrCodeDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(true, "TAXID123")).thenReturn(Mono.just(delegatorInternalUserId));


        Mono<MandateCreationResponse> result = mandateService.createMandateAppIo(delegateInternalUserId, "John", "Doe", it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet.PF, Mono.just(request));

        StepVerifier.create(result).expectError(PnMandateByHimselfException.class).verify();
    }

    @Test
    void createMandateAppIo_shouldReturn500_whenInternalError() {
        // Given
        MandateCreationRequest request = new MandateCreationRequest();
        request.setAarQrCodeValue("qrCodeValue");

        String decodedQr = "decodedQr";
        UserInfoQrCodeDto userInfoQrCodeDto = new UserInfoQrCodeDto();
        UserInfoDto userInfoDto = new UserInfoDto();
        userInfoDto.setTaxId("TAXID123");
        userInfoQrCodeDto.setRecipientInfo(userInfoDto);
        String delegatorInternalUserId = "internalUserId";
        MandateEntity entity = new MandateEntity();

        when(aarQrUtils.extractQrToken("qrCodeValue")).thenReturn(decodedQr);
        when(pnDeliveryClient.decodeAarQrCode(decodedQr)).thenReturn(Mono.just(userInfoQrCodeDto));
        when(pnDatavaultClient.ensureRecipientByExternalId(true, "TAXID123")).thenReturn(Mono.just(delegatorInternalUserId));
        when(mandateEntityBuilderMapper.buildMandateEntity(any(), any(), any(), any())).thenReturn(entity);
        when(pnDatavaultClient.updateMandateById(any(), any(), any(), any())).thenReturn(Mono.just("OK"));
        when(mandateDao.createMandate(entity, TypeSegregatorFilter.CIE)).thenReturn(Mono.error(new RuntimeException("Internal error")));

        Mono<MandateCreationResponse> result = mandateService.createMandateAppIo("cxId", "John", "Doe", it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet.PF, Mono.just(request));

        StepVerifier.create(result).expectError(RuntimeException.class).verify();
    }


    @Test
    void acceptMandateAppIo_success() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setDelegator("delegatorId");
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.PENDING));
        entity.setCreated(Instant.now().minus(Duration.ofMinutes(1)));
        entity.setValidationcode("valcode");
        entity.setAccepted(Instant.now());
        entity.setValidto(Instant.now());
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);

        when(mandateDao.retrieveMandateForDelegate("cxId", mandateId)).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(List.of("delegatorId"))).thenReturn(Flux.just(new BaseRecipientDtoDto().taxId("TAXID")));
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(5));
        when(pnMandateConfig.getCieValidToDuration()).thenReturn(Duration.ofDays(1));
        when(mandateDao.save(any())).thenReturn(Mono.just(entity));
        doNothing().when(cieCheckerAdapter).validateCie(any(), any(), any());

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .verifyComplete();
    }


    @Test
    void acceptMandateAppIo_error_mandateNotFound() {
        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.empty());
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, "mandateId", Mono.just(cieValidationData)))
                .expectError(PnMandateNotFoundException.class)
                .verify();
    }

    @Test
    void acceptMandateAppIo_error_workflowTypeNotCIE() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setDelegator("delegatorId");
        entity.setWorkflowType(WorkFlowType.STANDARD);
        entity.setState(10);
        entity.setCreated(Instant.now());
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        when(pnDatavaultClient.getRecipientDenominationByInternalId(any())).thenReturn(Flux.just(new BaseRecipientDtoDto().taxId("TAXID")));

        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.just(entity));
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(5));

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .expectError(PnInvalidMandateStatusException.class)
                .verify();
    }

    @Test
    void acceptMandateAppIo_error_statusNotPending() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setState(20); // State 10 is PENDING
        entity.setCreated(Instant.now());
        entity.setDelegator("delegator");
        entity.setDelegate("cxId");
        entity.setValidto(Instant.now());
        entity.setAccepted(Instant.now());
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(any())).thenReturn(Flux.just(new BaseRecipientDtoDto().taxId("TAXID")));
        doNothing().when(cieCheckerAdapter).validateCie(any(), any(), any());
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(5));
        when(mandateDao.save(any(MandateEntity.class))).thenReturn(Mono.just(new MandateEntity()));

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .expectError(PnInvalidMandateStatusException.class)
                .verify();
    }


    @Test
    void acceptMandateAppIo_error_mandateExpired() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setState(10);
        entity.setCreated(Instant.now().minus(Duration.ofHours(2)));
        entity.setDelegator("delegatorId");
        entity.setValidto(Instant.now());
        entity.setAccepted(Instant.now());
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.just(entity));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(any())).thenReturn(Flux.just(new BaseRecipientDtoDto().taxId("TAXID")));
        doNothing().when(cieCheckerAdapter).validateCie(any(), any(), any());
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(30));
        when(mandateDao.save(any(MandateEntity.class))).thenReturn(Mono.just(new MandateEntity()));

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .expectError(PnMandatePendingExpiredException.class)
                .verify();
    }

    @Test
    void acceptMandateAppIo_error_delegateTaxIdNotFound() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setState(20);
        entity.setCreated(Instant.now());
        entity.setDelegator("delegatorId");
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.just(entity));
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(30));
        when(pnDatavaultClient.getRecipientDenominationByInternalId(anyList())).thenReturn(Flux.empty());

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .expectError(PnInternalException.class)
                .verify();
    }

    @Test
    void acceptMandateAppIo_error_cieCheckerThrows() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setState(20);
        entity.setCreated(Instant.now());
        entity.setDelegator("delegatorId");
        entity.setValidationcode("valcode");
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.just(entity));
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(30));
        BaseRecipientDtoDto recipient = new BaseRecipientDtoDto().taxId("TAXID");
        when(pnDatavaultClient.getRecipientDenominationByInternalId(anyList())).thenReturn(Flux.just(recipient));
        doThrow(new RuntimeException("CIE error")).when(cieCheckerAdapter).validateCie(any(), any(), any());

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void acceptMandateAppIo_error_revokeMandateForInvalidVerificationCode() {
        String mandateId = "mid";
        MandateEntity entity = new MandateEntity();
        entity.setMandateId(mandateId);
        entity.setWorkflowType(WorkFlowType.CIE);
        entity.setState(20);
        entity.setCreated(Instant.now());
        entity.setDelegator("delegatorId");
        entity.setValidationcode("valcode");
        CIEValidationData cieValidationData = new CIEValidationData();
        NISData nisData = new NISData();
        nisData.setNis("testNIS");
        cieValidationData.setNisData(nisData);
        when(mandateDao.retrieveMandateForDelegate(anyString(), anyString())).thenReturn(Mono.just(entity));
        when(pnMandateConfig.getCiePendingDuration()).thenReturn(Duration.ofMinutes(30));
        BaseRecipientDtoDto recipient = new BaseRecipientDtoDto().taxId("TAXID");
        when(pnDatavaultClient.getRecipientDenominationByInternalId(anyList())).thenReturn(Flux.just(recipient));
        when(pnMandateConfig.getRevokeCieMandateOnVerificationFailure()).thenReturn(true);
        when(mandateDao.revokeMandate(anyString(), anyString(), any(), any())).thenReturn(Mono.just(entity));

        doThrow(new PnCieInvalidVerificationCodeException(ResultCieChecker.KO_EXC_NO_MATCH_NONCE_SIGNATURE))
                .when(cieCheckerAdapter).validateCie(any(), any(), any());

        StepVerifier.create(mandateService.acceptMandateAppIo("cxId", null, mandateId, Mono.just(cieValidationData)))
                .expectError(PnCieInvalidVerificationCodeException.class)
                .verify();
    }

}