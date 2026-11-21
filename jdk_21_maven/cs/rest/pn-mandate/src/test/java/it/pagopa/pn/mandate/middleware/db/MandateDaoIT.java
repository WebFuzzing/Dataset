package it.pagopa.pn.mandate.middleware.db;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.LocalStackTestConfig;
import it.pagopa.pn.mandate.MockAWSObjectsTestConfig;
import it.pagopa.pn.mandate.exceptions.PnInvalidVerificationCodeException;
import it.pagopa.pn.mandate.exceptions.PnMandateAlreadyExistsException;
import it.pagopa.pn.mandate.exceptions.PnMandateBadRequestException;
import it.pagopa.pn.mandate.exceptions.PnMandateNotFoundException;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.DelegateType;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateByDelegatorRequestDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto.StatusEnum;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.db.entities.MandateSupportEntity;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.model.WorkFlowType;
import it.pagopa.pn.mandate.utils.DateUtils;
import it.pagopa.pn.mandate.utils.RevocationCause;
import it.pagopa.pn.mandate.utils.TypeSegregatorFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Import;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;

import java.time.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Import({LocalStackTestConfig.class, MockAWSObjectsTestConfig.class})
public class MandateDaoIT extends AbstractTestConfiguration {

    private final Duration d = Duration.ofMillis(60000);

    @Autowired
    private MandateDao mandateDao;

    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    TestDao testDao;



    @BeforeEach
    void setup( @Value("${aws.dynamodb_table}") String table,
                @Value("${aws.dynamodb_table_history}") String tableHistory) {

        testDao = new TestDao( dynamoDbEnhancedAsyncClient, table, tableHistory);
    }

    @Test
    void createMandate() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.createMandate(mandateToInsert).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNotNull( elementFromDb);
            Assertions.assertEquals( mandateToInsert, elementFromDb);
        } catch (Exception e) {
           fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void createMandateAppIo() {
        //Given
        MandateEntity mandateToInsert = newMandateAppIo(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.createMandate(mandateToInsert, TypeSegregatorFilter.CIE).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateSupportEntity elementSupportFromDb = testDao.getSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            Assertions.assertNotNull(elementSupportFromDb);
            Assertions.assertNotNull( elementFromDb);
            Assertions.assertEquals( mandateToInsert, elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void createMandateAppioAlreadyExists() {
        //Given
        MandateEntity mandateToInsert = newMandateAppIo(false);
        MandateEntity mandateToInsert1 = newMandateAppIo(false);
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_2");

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert1.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.createMandate(mandateToInsert, TypeSegregatorFilter.CIE).block(d);
        Mono<MandateEntity> mono = mandateDao.createMandate(mandateToInsert1, TypeSegregatorFilter.CIE);
        assertThrows(PnMandateAlreadyExistsException.class, () -> mono.block(d));


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementFromDb1 = testDao.get(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());

            Assertions.assertNotNull( elementFromDb);
            Assertions.assertNull( elementFromDb1);
            Assertions.assertEquals( mandateToInsert, elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void createMandateAlreadyExists() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_2");

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert1.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.createMandate(mandateToInsert).block(d);
        Mono<MandateEntity> mono = mandateDao.createMandate(mandateToInsert1);
        assertThrows(PnMandateAlreadyExistsException.class, () -> mono.block(d));


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementFromDb1 = testDao.get(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());

            Assertions.assertNotNull( elementFromDb);
            Assertions.assertNull( elementFromDb1);
            Assertions.assertEquals( mandateToInsert, elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void createMandateAlreadyExistsForPG() {
        //Given
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegatorisperson(false);
        mandateToInsert1.setDelegate(mandateToInsert1.getDelegator());
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegatorisperson(false);
        mandateToInsert2.setDelegate(mandateToInsert2.getDelegator());
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.createMandate(mandateToInsert1).block(d);
        mandateDao.createMandate(mandateToInsert2).block(d);

        //Then
        try {
            MandateEntity elementFromDb1 = testDao.get(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            MandateEntity elementFromDb2 = testDao.get(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());

            Assertions.assertNotNull(elementFromDb1);
            Assertions.assertNotNull(elementFromDb2);
            Assertions.assertEquals(mandateToInsert1, elementFromDb1);
            Assertions.assertEquals(mandateToInsert2, elementFromDb2);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void createMandateStandardAndCie_NoConflict() {
        // Given
        MandateEntity mandateStandard = newMandate(false);
        MandateEntity mandateCie = newMandateAppIo(false);
        mandateCie.setWorkflowType(WorkFlowType.CIE);
        // Assicura che delegator e delegate coincidano per il test
        mandateCie.setDelegator(mandateStandard.getDelegator());
        mandateCie.setDelegate(mandateStandard.getDelegate());
        // Usa mandateId diversi per evitare collisioni sulla PK+SK
        mandateCie.setMandateId(mandateStandard.getMandateId() + "_CIE");
        mandateCie.setSk(mandateCie.getMandateId());

        try {
            testDao.delete(mandateStandard.getDelegator(), mandateStandard.getSk());
            testDao.delete(mandateCie.getDelegator(), mandateCie.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        // When
        mandateDao.createMandate(mandateStandard, TypeSegregatorFilter.STANDARD).block(d);
        mandateDao.createMandate(mandateCie, TypeSegregatorFilter.CIE).block(d);

        // Then
        try {
            MandateEntity elementStandard = testDao.get(mandateStandard.getDelegator(), mandateStandard.getSk());
            MandateEntity elementCie = testDao.get(mandateCie.getDelegator(), mandateCie.getSk());
            Assertions.assertNotNull(elementStandard);
            Assertions.assertNotNull(elementCie);
            Assertions.assertEquals(mandateStandard, elementStandard);
            Assertions.assertEquals(mandateCie, elementCie);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateStandard.getDelegator(), mandateStandard.getSk());
                testDao.delete(mandateCie.getDelegator(), mandateCie.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    public static MandateEntity newMandate(boolean withValidtoSetted) {
        MandateEntity m = new MandateEntity();
        m.setMandateId("f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1");
        m.setDelegatorUid("f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegator("PF-f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegate("PF-f271e4bf-0d69-4ed6-a39f-4ef2delegate");
        m.setDelegatorisperson(true);
        m.setDelegateisperson(true);
        m.setValidfrom(ZonedDateTime.of(LocalDateTime.of(2021, Month.DECEMBER, 14, 0, 0), ZoneId.of("Europe/Rome")).toInstant());
        m.setValidto(withValidtoSetted?Instant.now().plus(Duration.ofDays(5)):null);
        m.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.PENDING));
        m.setValidationcode("12345");
        m.setVisibilityIds(null);
        m.setWorkflowType(WorkFlowType.STANDARD);
        return m;
    }

    public static MandateEntity newMandateAppIo(boolean withValidtoSetted) {
        MandateEntity m = new MandateEntity();
        m.setMandateId("f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1");
        m.setDelegatorUid("f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegator("PF-f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegate("PF-f271e4bf-0d69-4ed6-a39f-4ef2delegate");
        m.setDelegatorisperson(true);
        m.setDelegateisperson(true);
        m.setValidfrom(ZonedDateTime.of(LocalDateTime.of(2021, Month.DECEMBER, 14, 0, 0), ZoneId.of("Europe/Rome")).toInstant());
        m.setValidto(withValidtoSetted?Instant.now().plus(Duration.ofDays(3)):null);
        m.setValidationcode("12345");
        m.setWorkflowType(WorkFlowType.CIE);
        m.setIuns(Set.of("QDYU-PUAD-QMQA-202305-G-3"));
        m.setCreated(Instant.now());
        return m;
    }

    public static MandateEntity newMandateWithGroups(boolean withValidtoSetted) {
        MandateEntity m = new MandateEntity();
        m.setMandateId("f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1");
        m.setDelegatorUid("f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegator("PF-f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegate("PF-f271e4bf-0d69-4ed6-a39f-4ef2delegate");
        m.setDelegatorisperson(true);
        m.setDelegateisperson(true);
        m.setGroups(new HashSet<>(List.of("f271e4bf-0d69-4ed6-a39f", "f271e4bf-0d69-4ed6-a50f")));
        m.setValidfrom(ZonedDateTime.of(LocalDateTime.of(2021, Month.DECEMBER, 14, 0, 0), ZoneId.of("Europe/Rome")).toInstant());
        m.setValidto(withValidtoSetted?Instant.now().plus(Duration.ofDays(5)):null);
        m.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        m.setValidationcode("12345");
        m.setVisibilityIds(null);
        m.setAccepted(Instant.now().minus(Duration.ofDays(5)));
        m.setWorkflowType(WorkFlowType.STANDARD);
        return m;
    }


    private MandateSupportEntity newMandateSupport(MandateEntity source) {
        return new MandateSupportEntity(source);
    }

    @Test
    void listMandatesByDelegatePG() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setGroups(Set.of("RECLAMI"));
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");

        MandateEntity mandateToInsert3 = newMandate(false);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setGroups(Set.of("PIPPO"));
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert.getDelegate())
                .cxType(CxTypeAuthFleet.PG)
                .groups(List.of("RECLAMI"))
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD)
                .collectList()
                .block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertFalse(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertFalse(results.contains(mandateToInsert2));
            Assertions.assertFalse(results.contains(mandateToInsert3));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegate() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(false);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert.getDelegate())
                .cxType(CxTypeAuthFleet.PF)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(4, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertTrue(results.contains(mandateToInsert2));
            Assertions.assertTrue(results.contains(mandateToInsert3));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateWithMandateId() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(false);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert.getDelegate())
                .mandateId(mandateToInsert1.getMandateId())
                .cxType(CxTypeAuthFleet.PF)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert1));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateWithMandateId2() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        mandateToInsert2.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert3 = newMandate(false);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert2.getDelegate())
                .mandateId(mandateToInsert2.getMandateId())
                .cxType(CxTypeAuthFleet.PF)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert2));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateWithMandateId3() {
        //Given
        MandateEntity mandateToInsert = newMandate(true);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert1 = newMandate(true);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(true);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        mandateToInsert2.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert3 = newMandate(true);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert2.getDelegate())
                .mandateId(mandateToInsert2.getMandateId())
                .cxType(CxTypeAuthFleet.PF)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert2));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateActiveOnly() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(false);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert.getDelegate())
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(2, results.size());
            Assertions.assertFalse(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertFalse(results.contains(mandateToInsert2));
            Assertions.assertTrue(results.contains(mandateToInsert3));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void listMandatesByDelegateActiveOnlyAndNotExpired() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(false);
        mandateToInsert3.setDelegator(mandateToInsert3.getDelegator() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert3.setValidto(DateUtils.atStartOfDay(Instant.now()).minusSeconds(1).toInstant());

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(mandateToInsert.getDelegate())
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, TypeSegregatorFilter.STANDARD).collectList().block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertFalse(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertFalse(results.contains(mandateToInsert2));
            Assertions.assertFalse(results.contains(mandateToInsert3));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateV2_caseIuns_Ok() {
        /*
            Given
            2 Deleghe attive:
            - una senza workflowType (considerabile come STANDARD)
            - una con workflowType di tipo CIE e legata ad uno specifico IUN
        */
        String iun = "TEST_IUN_1234567890";
        MandateEntity mandateToInsert1 = newMandate(false);
        String delegate = mandateToInsert1.getDelegate();
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        mandateToInsert2.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert2.setWorkflowType(WorkFlowType.CIE);
        mandateToInsert2.setIuns(Set.of(iun));

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        /*
            When
            La ricerca viene effettuata specificando lo IUN legato alla Delega con workflowType CIE
            e lo status ACTIVE.
         */
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(delegate)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .iun(iun)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, null).collectList().block(d);

        /*
            Then
            Vengono ritornate entrambe le deleghe attive in quanto:
            - la prima delega senza workflowType viene sempre considerata come STANDARD e quindi ricade nei criteri di ricerca
            - la seconda delega con workflowType CIE viene considerata in quanto lo IUN specificato nella ricerca
              corrisponde a quello associato alla delega stessa
         */
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(2, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertTrue(results.contains(mandateToInsert2));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateV2_caseIuns_Ko() {
        /*
            Given
            2 Deleghe attive:
            - una senza workflowType (considerabile come STANDARD)
            - una con workflowType di tipo CIE e legata ad uno specifico IUN
        */
        String iun = "TEST_IUN_1234567890";
        MandateEntity mandateToInsert1 = newMandate(false);
        String delegate = mandateToInsert1.getDelegate();
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setDelegator(mandateToInsert2.getDelegator() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        mandateToInsert2.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert2.setWorkflowType(WorkFlowType.CIE);
        mandateToInsert2.setIuns(Set.of(iun));

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        /*
            When
            La ricerca viene effettuata specificando uno IUN NON legato alla Delega con workflowType CIE
            e lo status ACTIVE.
         */
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(delegate)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .iun("OTHER_IUN_0987654321")
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, null).collectList().block(d);

        /*
            Then
            Viene ritornata solo la prima delega attiva in quanto:
            - la prima delega senza workflowType viene sempre considerata come STANDARD e quindi ricade nei criteri di ricerca
            - la seconda delega con workflowType CIE non viene considerata in quanto lo IUN specificato nella ricerca
              NON corrisponde a quello associato alla delega stessa
         */
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertFalse(results.contains(mandateToInsert2));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateV2_caseNotificationSentAt_Ok() {
        /*
            Given
            1 Delega attiva senza workflowType (considerabile come STANDARD)
        */
        MandateEntity mandateToInsert1 = newMandate(false);
        String delegate = mandateToInsert1.getDelegate();
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        /*
            When
            La ricerca viene effettuata specificando il notificationSentAt legato ad una notifica
         */
        Instant notificationSentAt = mandateToInsert1.getValidfrom().plus(Duration.ofDays(2)); // Data successiva alla validFrom della delega
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(delegate)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .notificationSentAt(notificationSentAt)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, null).collectList().block(d);

        /*
            Then
            Viene ritornata la delega in quanto la delega attiva esistente ha una validFrom precedente rispetto
            alla data di notificationSentAt specificata nella ricerca.
         */
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert1));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateV2_caseNotificationSentAt_Ko() {
        /*
            Given
            1 Delega attiva senza workflowType (considerabile come STANDARD)
        */
        MandateEntity mandateToInsert1 = newMandate(false);
        String delegate = mandateToInsert1.getDelegate();
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        /*
            When
            La ricerca viene effettuata specificando il notificationSentAt legato ad una notifica
         */
        Instant notificationSentAt = mandateToInsert1.getValidfrom().minus(Duration.ofDays(2)); // Data precedente alla validFrom della delega
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(delegate)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .notificationSentAt(notificationSentAt)
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, null).collectList().block(d);

        /*
            Then
            Non viene ritornata nessuna delega in quanto la delega attiva esistente ha una validFrom successiva rispetto
            alla data di notificationSentAt specificata nella ricerca.
         */
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(0, results.size());
            Assertions.assertTrue(results.isEmpty());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateV2_caseVisibilityIds_Ok() {
        /*
            Given
            1 Delega attiva senza workflowType (considerabile come STANDARD)
        */
        MandateEntity mandateToInsert1 = newMandate(false);
        String delegate = mandateToInsert1.getDelegate();
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert1.setVisibilityIds(Set.of("ID_1", "ID_2", "ID_3"));

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        /*
            When
            La ricerca viene effettuata specificando un rootSenderId (mittente) legato alla visibilità della delega
         */
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(delegate)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .rootSenderId("ID_2")
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, null).collectList().block(d);

        /*
            Then
            Viene ritornata la delega in quanto la delega attiva esistente ha tra i suoi visibilityIds l'ID specificato
            nella ricerca.
         */
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert1));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegateV2_caseVisibilityIds_Ko() {
        /*
            Given
            1 Delega attiva senza workflowType (considerabile come STANDARD)
        */
        MandateEntity mandateToInsert1 = newMandate(false);
        String delegate = mandateToInsert1.getDelegate();
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert1.setVisibilityIds(Set.of("ID_1", "ID_2", "ID_3"));

        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        /*
            When
            La ricerca viene effettuata specificando un rootSenderId (mittente) legato alla visibilità della delega
         */
        InputSearchMandateDto inputSearchMandateDto = InputSearchMandateDto.builder()
                .delegateId(delegate)
                .status(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE))
                .cxType(CxTypeAuthFleet.PF)
                .rootSenderId("NON_EXISTING_ID")
                .build();
        List<MandateEntity> results = mandateDao.listMandatesByDelegate(inputSearchMandateDto, null).collectList().block(d);

        /*
            Then
            Non viene ritornata nessuna delega in quanto la delega attiva esistente non ha tra i suoi visibilityIds l'ID specificato
            nella ricerca.
         */
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(0, results.size());
            Assertions.assertTrue(results.isEmpty());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegator() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegate(mandateToInsert1.getDelegate() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(true);
        mandateToInsert2.setDelegate(mandateToInsert2.getDelegate() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(true);
        mandateToInsert3.setDelegate(mandateToInsert3.getDelegate() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        List<MandateEntity> results = mandateDao.listMandatesByDelegator(mandateToInsert.getDelegator(), null, null, null)
                .collectList()
                .block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(4, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertTrue(results.contains(mandateToInsert2));
            Assertions.assertTrue(results.contains(mandateToInsert3));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegatorOneCIE() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegate(mandateToInsert1.getDelegate() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(true);
        mandateToInsert2.setDelegate(mandateToInsert2.getDelegate() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(true);
        mandateToInsert3.setDelegate(mandateToInsert3.getDelegate() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        // Mandato con workflowType "CIE"
        MandateEntity mandateCIE = newMandate(false);
        mandateCIE.setDelegate(mandateCIE.getDelegate() + "_cie");
        mandateCIE.setMandateId(mandateCIE.getMandateId() + "_cie");
        mandateCIE.setWorkflowType(WorkFlowType.CIE);
        mandateCIE.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
            testDao.delete(mandateCIE.getDelegator(), mandateCIE.getSk());
            mandateDao.createMandate(mandateCIE).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        List<MandateEntity> results = mandateDao.listMandatesByDelegator(mandateToInsert.getDelegator(), null, null, null)
                .collectList()
                .block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(4, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertTrue(results.contains(mandateToInsert2));
            Assertions.assertTrue(results.contains(mandateToInsert3));
            Assertions.assertFalse(results.contains(mandateCIE));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
                testDao.delete(mandateCIE.getDelegator(), mandateCIE.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegatorWithMandateId() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegate(mandateToInsert1.getDelegate() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(true);
        mandateToInsert2.setDelegate(mandateToInsert2.getDelegate() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(true);
        mandateToInsert3.setDelegate(mandateToInsert3.getDelegate() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        List<MandateEntity> results = mandateDao.listMandatesByDelegator(mandateToInsert.getDelegator(), null, mandateToInsert1.getMandateId(), null)
                .collectList()
                .block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals(1, results.size());
            Assertions.assertTrue(results.contains(mandateToInsert1));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegatorOnlyActive() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegate(mandateToInsert1.getDelegate() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert2 = newMandate(true);
        mandateToInsert2.setDelegate(mandateToInsert2.getDelegate() + "_2");
        mandateToInsert2.setMandateId(mandateToInsert2.getMandateId() + "_2");
        MandateEntity mandateToInsert3 = newMandate(true);
        mandateToInsert3.setDelegate(mandateToInsert3.getDelegate() + "_3");
        mandateToInsert3.setMandateId(mandateToInsert3.getMandateId() + "_3");
        mandateToInsert3.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert2).block(d);
            testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            mandateDao.createMandate(mandateToInsert3).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        List<MandateEntity> results = mandateDao.listMandatesByDelegator(mandateToInsert.getDelegator(), StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), null, null)
                .collectList()
                .block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals( 2, results.size());
            Assertions.assertFalse(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
            Assertions.assertFalse(results.contains(mandateToInsert2));
            Assertions.assertTrue(results.contains(mandateToInsert3));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
                testDao.delete(mandateToInsert3.getDelegator(), mandateToInsert3.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void listMandatesByDelegatorWithType() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setDelegateisperson(true);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setDelegateisperson(false);
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        List<MandateEntity> results = mandateDao.listMandatesByDelegator(mandateToInsert.getDelegator(), StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), null, DelegateType.PG)
                .collectList()
                .block(d);

        //Then
        try {
            Assertions.assertNotNull(results);
            Assertions.assertEquals( 1, results.size());
            Assertions.assertFalse(results.contains(mandateToInsert));
            Assertions.assertTrue(results.contains(mandateToInsert1));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void acceptMandateNoExpiration() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), null, CxTypeAuthFleet.PF).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void acceptMandate_shouldThrowBadRequest_whenMandateNotStandardSegregation() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setWorkflowType(WorkFlowType.CIE);
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(),null, CxTypeAuthFleet.PF).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Mono<MandateEntity> mono = mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), null, CxTypeAuthFleet.PF);
        assertThrows(PnMandateBadRequestException.class, () -> mono.block(d));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

    }

    @Test
    void acceptMandateAlreadyAccepted() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
            mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(),null, CxTypeAuthFleet.PF).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(),null, CxTypeAuthFleet.PF).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void acceptMandateWrongState() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.EXPIRED));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Mono<MandateEntity> mono = mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), null, CxTypeAuthFleet.PF);
        assertThrows(PnInternalException.class, () -> mono.block(d));


        //Then
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }

    @Test
    void acceptMandateInvalidCode() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        String wrongcode = "99999";

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Mono<MandateEntity> mono = mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), wrongcode, null, CxTypeAuthFleet.PF);
        assertThrows(PnInvalidVerificationCodeException.class, () -> mono.block(d));


        //Then
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }


    @Test
    void acceptMandateNotFoud() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        String wrongdelegate = mandateToInsert.getDelegate() + "_wrong";
        String code = mandateToInsert.getValidationcode();

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Mono<MandateEntity> mono = mandateDao.acceptMandate(wrongdelegate, mandateToInsert.getMandateId(), code, null, CxTypeAuthFleet.PF);
        assertThrows(PnMandateNotFoundException.class, () -> mono.block(d));


        //Then
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }


    @Test
    void acceptMandateExpiration() {
        //Given
        MandateEntity mandateToInsert = newMandate(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), null,CxTypeAuthFleet.PF).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateSupportEntity elementSupportFromDb = testDao.getSupport(mandateSupport.getDelegator(), mandateSupport.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());
            Assertions.assertNotNull(elementSupportFromDb);
            Assertions.assertEquals( mandateSupport, elementSupportFromDb);

        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void acceptMandatePgNullGroup() {
        //Given
        MandateEntity mandateToInsert = newMandate(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), null, CxTypeAuthFleet.PG).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateSupportEntity elementSupportFromDb = testDao.getSupport(mandateSupport.getDelegator(), mandateSupport.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());
            Assertions.assertNotNull(elementSupportFromDb);
            Assertions.assertEquals(mandateSupport, elementSupportFromDb);

        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void acceptMandatePgEmptyGroup() {
        //Given
        MandateEntity mandateToInsert = newMandate(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), Collections.emptyList(), CxTypeAuthFleet.PG).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateSupportEntity elementSupportFromDb = testDao.getSupport(mandateSupport.getDelegator(), mandateSupport.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());
            Assertions.assertNotNull(elementSupportFromDb);
            Assertions.assertEquals(mandateSupport, elementSupportFromDb);

        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void acceptMandatePgGroup() {
        //Given
        MandateEntity mandateToInsert = newMandate(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), List.of("G1"), CxTypeAuthFleet.PG).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateSupportEntity elementSupportFromDb = testDao.getSupport(mandateSupport.getDelegator(), mandateSupport.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());
            Assertions.assertNotNull(elementSupportFromDb);
            Assertions.assertEquals(mandateSupport, elementSupportFromDb);
            Assertions.assertTrue(elementFromDb.getGroups().contains("G1"));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void updateMandate(){
        //Given
        MandateEntity mandateToInsert = newMandateWithGroups(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.updateMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getGroups()).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateSupportEntity elementSupportFromDb = testDao.getSupport(mandateSupport.getDelegator(), mandateSupport.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertNotNull(elementFromDb.getAccepted());
            Assertions.assertNotNull(elementSupportFromDb);
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void updateMandate_shouldThrowBadRequest_whenWorkflowTypeIsCIE() {
        // Given
        MandateEntity cieMandate = newMandateWithGroups(true);
        cieMandate.setWorkflowType(WorkFlowType.CIE);
        MandateSupportEntity mandateSupport = newMandateSupport(cieMandate);

        try {
            testDao.delete(cieMandate.getDelegator(), cieMandate.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(cieMandate).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        // When & Then
        Assertions.assertThrows(
                PnMandateBadRequestException.class,
                () -> mandateDao.updateMandate(
                        cieMandate.getDelegate(),
                        cieMandate.getMandateId(),
                        cieMandate.getGroups()
                ).block(d)
        );

        // Cleanup
        try {
            testDao.delete(cieMandate.getDelegator(), cieMandate.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }

    @Test
    void updateMandateNotActive() {
        // Given
        MandateEntity mandateToInsert = newMandateWithGroups(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);
        // Imposta lo stato a EXPIRED (non attivo)
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.EXPIRED));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        // When & Then
        Assertions.assertThrows(
            it.pagopa.pn.mandate.exceptions.PnInvalidMandateStatusException.class,
            () -> mandateDao.updateMandate(
                    mandateToInsert.getDelegate(),
                    mandateToInsert.getMandateId(),
                    mandateToInsert.getGroups()
            ).block(d)
        );

        // Cleanup
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }

    @Test
    void updateMandateExpiredValidTo() {
        // Given
        MandateEntity mandateToInsert = newMandateWithGroups(true);
        MandateSupportEntity mandateSupport = newMandateSupport(mandateToInsert);
        // Stato ACTIVE ma validto già scaduto
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert.setValidto(Instant.now().minusSeconds(3600)); // 1 ora fa

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        // When & Then
        Assertions.assertThrows(
            it.pagopa.pn.mandate.exceptions.PnMandateNotFoundException.class,
            () -> mandateDao.updateMandate(
                    mandateToInsert.getDelegate(),
                    mandateToInsert.getMandateId(),
                    mandateToInsert.getGroups()
            ).block(d)
        );

        // Cleanup
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteSupport(mandateSupport.getDelegator(), mandateSupport.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }

    @Test
    void rejectMandate() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.rejectMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId()).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNull( elementFromDb);
            Assertions.assertNotNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.REJECTED), elementHistoryFromDb.getState());
            Assertions.assertNotNull(elementHistoryFromDb.getRejected());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void rejectMandateActive() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.rejectMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId()).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNull( elementFromDb);
            Assertions.assertNotNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.REJECTED), elementHistoryFromDb.getState());
            Assertions.assertNotNull(elementHistoryFromDb.getRejected());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void rejectMandate_shouldThrowBadRequest_whenWorkflowTypeIsCIE() {
        // Given
        MandateEntity cieMandate = newMandate(false);
        cieMandate.setWorkflowType(WorkFlowType.CIE);
        cieMandate.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(cieMandate.getDelegator(), cieMandate.getSk());
            mandateDao.createMandate(cieMandate).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        // When & Then
        Assertions.assertThrows(
            PnMandateBadRequestException.class,
            () -> mandateDao.rejectMandate(cieMandate.getDelegate(), cieMandate.getMandateId()).block(d)
        );

        // Cleanup
        try {
            testDao.delete(cieMandate.getDelegator(), cieMandate.getSk());
            testDao.deleteHistory(cieMandate.getDelegator(), cieMandate.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }

    @Test
    void rejectMandateNotFound() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        String wrongdelegate = mandateToInsert.getDelegate() + "_WRONG";

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        Mono<MandateEntity> mono =  mandateDao.rejectMandate(wrongdelegate, mandateToInsert.getMandateId());
        assertThrows(PnMandateNotFoundException.class, () -> mono.block(d));


        //Then
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }

    @Test
    void revokeMandate() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.revokeMandate(mandateToInsert.getDelegator(), mandateToInsert.getMandateId(), TypeSegregatorFilter.ALL, RevocationCause.USER).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNull( elementFromDb);
            Assertions.assertNotNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.REVOKED), elementHistoryFromDb.getState());
            Assertions.assertNotNull(elementHistoryFromDb.getRevoked());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void revokeMandateCie_shouldThrowBadRequestIfWorkflowTypeIsNotIncludedInSegregator() {
        // Given
        MandateEntity cieMandate = newMandate(false);
        cieMandate.setWorkflowType(WorkFlowType.CIE);

        try {
            testDao.delete(cieMandate.getDelegator(), cieMandate.getSk());
            mandateDao.createMandate(cieMandate).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        // When & Then
        Assertions.assertThrows(
            PnMandateBadRequestException.class,
            () -> mandateDao.revokeMandate(cieMandate.getDelegator(), cieMandate.getMandateId(), TypeSegregatorFilter.STANDARD, RevocationCause.USER).block(d)
        );

        // Cleanup
        try {
            testDao.delete(cieMandate.getDelegator(), cieMandate.getSk());
            testDao.deleteHistory(cieMandate.getDelegator(), cieMandate.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
    }


    @Test
    void revokeMandateTwice() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }
        mandateDao.revokeMandate(mandateToInsert.getDelegator(), mandateToInsert.getMandateId(), TypeSegregatorFilter.STANDARD, RevocationCause.USER).block(d);


        //When
        mandateDao.revokeMandate(mandateToInsert.getDelegator(), mandateToInsert.getMandateId(), TypeSegregatorFilter.STANDARD, RevocationCause.USER).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNull( elementFromDb);
            Assertions.assertNotNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.REVOKED), elementHistoryFromDb.getState());
            Assertions.assertNotNull(elementHistoryFromDb.getRevoked());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void expireMandateActiveStillValid() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE));
        mandateToInsert.setValidto(Instant.now().plus(Duration.ofHours(1)));
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.expireMandate(mandateToInsert.getDelegator(), mandateToInsert.getDelegatorUid(), mandateToInsert.getDelegatorisperson()?"PF":"PG", mandateToInsert.getMandateId()).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNotNull( elementFromDb);
            Assertions.assertNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE), elementFromDb.getState());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void expireMandateActiveAndExpired() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE));
        mandateToInsert.setValidto(Instant.now());
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.expireMandate(mandateToInsert.getDelegator(), mandateToInsert.getDelegatorUid(), mandateToInsert.getDelegatorisperson()?"PF":"PG", mandateToInsert.getMandateId()).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNull( elementFromDb);
            Assertions.assertNotNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.EXPIRED), elementHistoryFromDb.getState());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void expireMandatePending() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.expireMandate(mandateToInsert.getDelegator(), mandateToInsert.getDelegatorUid(), mandateToInsert.getDelegatorisperson()?"PF":"PG", mandateToInsert.getMandateId()).block(d);


        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            MandateEntity elementHistoryFromDb = testDao.getHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNull( elementFromDb);
            Assertions.assertNotNull( elementHistoryFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(StatusEnum.PENDING), elementHistoryFromDb.getState());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.deleteHistory(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void expireMandateNotFound() {
        //Given
        // nothing

        //When
        Mono<MandateEntity> mono = mandateDao.expireMandate("fake", "fake", "PF", "fake");
        assertThrows(PnMandateNotFoundException.class, () -> mono.block(d));
    }

    @Test
    void listMandatesByDelegators() {
        MandateEntity mandateToInsert = newMandate(true);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        MandateByDelegatorRequestDto requestDto = new MandateByDelegatorRequestDto();
        requestDto.setDelegatorId(mandateToInsert.getDelegator());
        requestDto.setMandateId(mandateToInsert.getMandateId());

        try {
            List<MandateEntity> result = mandateDao.listMandatesByDelegators(List.of(requestDto))
                    .collectList()
                    .block(d);

            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.size());
            Assertions.assertEquals(mandateToInsert, result.get(0));
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void searchByDelegate1() {
        MandateEntity mandateToInsert1 = newMandate(false);
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setMandateId(mandateToInsert1.getMandateId() + "-2");
        mandateToInsert2.setDelegator(mandateToInsert1.getDelegator() + "-2");
        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            mandateDao.createMandate(mandateToInsert2).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        String delegateId = mandateToInsert1.getDelegate();
        try {
            Page<MandateEntity> result = mandateDao.searchByDelegate(delegateId, null, null, null, 1, null)
                    .block(d);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.items().size());
            Assertions.assertNotNull(result.lastEvaluatedKey());
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void searchByDelegate2() {
        MandateEntity mandateToInsert1 = newMandate(false);
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert1.setGroups(Set.of("G1"));
        MandateEntity mandateToInsert2 = newMandate(false);
        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            mandateDao.createMandate(mandateToInsert2).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        String delegateId = mandateToInsert1.getDelegate();
        String mandateId = mandateToInsert1.getMandateId();
        Integer state = mandateToInsert1.getState();
        try {
            Page<MandateEntity> result = mandateDao.searchByDelegate(delegateId, state, List.of("G1", "G2"), List.of("PF-f271e4bf-0d69-4ed6-a39f-4efdelegator","other"), 2, null)
                    .block(d);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.items().size());
            Assertions.assertEquals(mandateId, result.items().get(0).getMandateId());
            Assertions.assertNull(result.lastEvaluatedKey());
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void searchByDelegate3() {
        MandateEntity mandateToInsert1 = newMandate(false);
        MandateEntity mandateToInsert2 = newMandate(false);
        mandateToInsert2.setMandateId(mandateToInsert1.getMandateId() + "-2");
        mandateToInsert2.setDelegator(mandateToInsert1.getDelegator() + "-2");
        try {
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            mandateDao.createMandate(mandateToInsert1).block(d);
            mandateDao.createMandate(mandateToInsert2).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        String delegateId = mandateToInsert1.getDelegate();
        try {
            Page<MandateEntity> result1 = mandateDao.searchByDelegate(delegateId, null, null, null, 1, null)
                    .block(d);
            Assertions.assertNotNull(result1);
            Assertions.assertNotNull(result1.lastEvaluatedKey());
            Assertions.assertEquals(1, result1.items().size());

            PnLastEvaluatedKey lastEvaluatedKey = new PnLastEvaluatedKey();
            lastEvaluatedKey.setInternalLastEvaluatedKey(result1.lastEvaluatedKey());
            Page<MandateEntity> result2 = mandateDao.searchByDelegate(delegateId, null, null, null, 2, lastEvaluatedKey)
                    .block(d);
            Assertions.assertNotNull(result2);
            Assertions.assertEquals(1, result2.items().size());
            Assertions.assertNull(result2.lastEvaluatedKey());
        } finally {
            try {
                testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
                testDao.delete(mandateToInsert2.getDelegator(), mandateToInsert2.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void searchByDelegateExcludesCIEWorkflowType() {
        // Inserisce una delega STANDARD
        MandateEntity mandateStandard = newMandate(false);
        mandateStandard.setWorkflowType(WorkFlowType.STANDARD);
        // Inserisce una delega CIE
        MandateEntity mandateCIE = newMandate(false);
        mandateCIE.setMandateId(mandateStandard.getMandateId() + "-CIE");
        mandateCIE.setDelegator(mandateStandard.getDelegator() + "-CIE");
        mandateCIE.setWorkflowType(WorkFlowType.CIE);

        try {
            testDao.delete(mandateStandard.getDelegator(), mandateStandard.getSk());
            testDao.delete(mandateCIE.getDelegator(), mandateCIE.getSk());
            mandateDao.createMandate(mandateStandard).block(d);
            mandateDao.createMandate(mandateCIE).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        String delegateId = mandateStandard.getDelegate();
        try {
            Page<MandateEntity> result = mandateDao.searchByDelegate(delegateId, null, null, null, 10, null).block(d);
            Assertions.assertNotNull(result);
            Assertions.assertEquals(1, result.items().size());
            Assertions.assertTrue(result.items().contains(mandateStandard));
            Assertions.assertFalse(result.items().contains(mandateCIE));
        } finally {
            try {
                testDao.delete(mandateStandard.getDelegator(), mandateStandard.getSk());
                testDao.delete(mandateCIE.getDelegator(), mandateCIE.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void createMandateWithCreated() {
        //Given
        MandateEntity mandateToInsert = newMandate(true);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.createMandate(mandateToInsert).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNotNull( elementFromDb);
            Assertions.assertNotNull( elementFromDb.getCreated());
            Assertions.assertEquals( mandateToInsert, elementFromDb);
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }

    @Test
    void acceptMandateWithExpiration() {
        //Given
        MandateEntity mandateToInsert = newMandate(false);
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(d);
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        mandateDao.acceptMandate(mandateToInsert.getDelegate(), mandateToInsert.getMandateId(), mandateToInsert.getValidationcode(), null, CxTypeAuthFleet.PF).block(d);

        //Then
        try {
            MandateEntity elementFromDb = testDao.get(mandateToInsert.getDelegator(), mandateToInsert.getSk());

            Assertions.assertNotNull(elementFromDb);
            Assertions.assertEquals( StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE), elementFromDb.getState());
            Assertions.assertNotNull(elementFromDb.getAccepted());


            MandateSupportEntity support = testDao.getSupport(elementFromDb.getDelegator(), MandateSupportEntity.MANDATE_TRIGGERHELPER_PREFIX+elementFromDb.getMandateId());
            Assertions.assertNotNull(support.getTtl());
        } catch (Exception e) {
            throw new RuntimeException();
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }
}