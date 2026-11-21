package it.pagopa.pn.mandate.middleware.db;

import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.LocalStackTestConfig;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.middleware.db.entities.DelegateEntity;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.model.WorkFlowType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;


@ExtendWith(SpringExtension.class)
@Import(LocalStackTestConfig.class)
class DelegateDaoIT extends AbstractTestConfiguration {

    @Autowired
    private MandateDao mandateDao;

    @Autowired
    private DelegateDao delegateDao;

    @Autowired
    PnAuditLogBuilder pnAuditLogBuilder;
    @Autowired
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;


    TestDao testDao;

    @BeforeEach
    void setup( @Value("${aws.dynamodb_table}") String table,
                @Value("${aws.dynamodb_table_history}") String tableHistory) {

        testDao = new TestDao( dynamoDbEnhancedAsyncClient, table, tableHistory);
    }

    @Test
    void countMandatesPendingOnly() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(false);

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, null)
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(1,  result.getPendingcount());
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
    void countMandatesPendingOnlyExceptCIE() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(false);
        MandateEntity mandateToInsertCIE = MandateDaoIT.newMandate(false);
        mandateToInsertCIE.setWorkflowType(WorkFlowType.CIE);
        mandateToInsertCIE.setMandateId(mandateToInsertCIE.getMandateId() + "_CIE");
        mandateToInsertCIE.setDelegator(mandateToInsertCIE.getDelegator() + "_CIE");
        mandateToInsertCIE.setWorkflowType(WorkFlowType.CIE);
        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            testDao.delete(mandateToInsertCIE.getDelegator(), mandateToInsertCIE.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
            mandateDao.createMandate(mandateToInsertCIE).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, null)
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(1,  result.getPendingcount());
        } catch (Exception e) {
            fail(e);
        } finally {
            try {
                testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
                testDao.delete(mandateToInsertCIE.getDelegator(), mandateToInsertCIE.getSk());
            } catch (Exception e) {
                System.out.println("Nothing to remove");
            }
        }
    }


    @Test
    void countMandatesPendingExpiredOnly() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        mandateToInsert.setValidto(Instant.now().minusSeconds(86400));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, null)
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(0,  result.getPendingcount());
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
    void countMandatesPendingExpiredOnly_withgroups() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        mandateToInsert.setValidto(Instant.now().minusSeconds(86400));
        mandateToInsert.setGroups(Set.of("AMMINISTRAZIONE"));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, List.of("AMMINISTRAZIONE"))
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(0,  result.getPendingcount());
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
    void countMandatesPendingOnly_withgroups() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(true);
        mandateToInsert.setGroups(Set.of("AMMINISTRAZIONE"));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, List.of("AMMINISTRAZIONE"))
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(1,  result.getPendingcount());
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
    void countMandatesPendingAndActive() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(false);
        MandateEntity mandateToInsert1 = MandateDaoIT.newMandate(false);
        mandateToInsert1.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));
        mandateToInsert1.setDelegator(mandateToInsert1.getDelegator() + "_1");
        mandateToInsert1.setMandateId(mandateToInsert1.getMandateId() + "_1");

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
            testDao.delete(mandateToInsert1.getDelegator(), mandateToInsert1.getSk());
            mandateDao.createMandate(mandateToInsert1).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, null)
                .block(Duration.ofMillis(3000));


        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(1,  result.getPendingcount());
        } catch (Exception e) {
            fail(e);
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
    void countMandatesActiveOnly() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PF, null)
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(0,  result.getPendingcount());
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
    void countMandatesActiveOnlyPG() {
        //Given
        MandateEntity mandateToInsert = MandateDaoIT.newMandate(false);
        mandateToInsert.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.ACTIVE));

        try {
            testDao.delete(mandateToInsert.getDelegator(), mandateToInsert.getSk());
            mandateDao.createMandate(mandateToInsert).block(Duration.ofMillis(3000));
        } catch (Exception e) {
            System.out.println("Nothing to remove");
        }

        //When
        DelegateEntity result = delegateDao.countMandates(mandateToInsert.getDelegate(), CxTypeAuthFleet.PG, List.of("RECLAMI"))
                .block(Duration.ofMillis(3000));

        //Then
        try {
            Assertions.assertNotNull( result);
            Assertions.assertEquals(0,  result.getPendingcount());
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
}