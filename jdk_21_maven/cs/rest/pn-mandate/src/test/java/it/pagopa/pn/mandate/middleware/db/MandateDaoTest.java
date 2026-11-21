package it.pagopa.pn.mandate.middleware.db;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.LocalStackTestConfig;
import it.pagopa.pn.mandate.MockAWSObjectsTestConfig;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateByDelegatorRequestDto;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPage;
import software.amazon.awssdk.enhanced.dynamodb.model.BatchGetResultPagePublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@Import({LocalStackTestConfig.class, MockAWSObjectsTestConfig.class})
class MandateDaoTest extends AbstractTestConfiguration {

    @Mock
    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    @Mock
    DynamoDbAsyncClient dynamoDbAsyncClient;
    @Mock
    PnMandateConfig pnMandateConfig;

    @InjectMocks
    MandateDao mandateDao;

    @Test
    @Disabled("non è possibile creare un mock di BatchGetResultPage")
    void testListMandatesByDelegators() {
        BatchGetResultPage page = mock(BatchGetResultPage.class);
        when(page.resultsForTable(any()))
                .thenReturn(Collections.emptyList());
        when(page.unprocessedKeysForTable(any()))
                .thenReturn(List.of(Key.builder().partitionValue("PK1").sortValue("MANDATE#M1").build()))
                .thenReturn(Collections.emptyList());
        when(dynamoDbEnhancedAsyncClient.batchGetItem((BatchGetItemEnhancedRequest) any()))
                .thenReturn(BatchGetResultPagePublisher.create(SdkPublisher.adapt(Mono.just(page))));

        MandateByDelegatorRequestDto dto1 = new MandateByDelegatorRequestDto();
        dto1.setDelegatorId("PK1");
        dto1.setMandateId("M1");
        MandateByDelegatorRequestDto dto2 = new MandateByDelegatorRequestDto();
        dto2.setDelegatorId("PK2");
        dto2.setMandateId("M2");
        List<MandateByDelegatorRequestDto> requestDto = List.of(dto1, dto2);

        List<MandateEntity> result = mandateDao.listMandatesByDelegators(requestDto).collectList().block(Duration.ofMillis(3000));
        assertNotNull(result);
        verify(dynamoDbEnhancedAsyncClient, times(2)).batchGetItem((BatchGetItemEnhancedRequest) any());
    }

}
