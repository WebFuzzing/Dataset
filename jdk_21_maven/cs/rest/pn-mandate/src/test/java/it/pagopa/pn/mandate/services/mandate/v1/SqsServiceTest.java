package it.pagopa.pn.mandate.services.mandate.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.EventType;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SqsServiceTest {

    @Test
    void testPush() {
        SqsClient amazonSQS = mock(SqsClient.class);
        GetQueueUrlResponse getQueueUrlResponse = GetQueueUrlResponse.builder().queueUrl("queueUrl").build();

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);

        SqsService sqsService = new SqsService("queueNameTest", amazonSQS, new ObjectMapper());

        MandateEntity mandateEntity = new MandateEntity("", "");
        assertDoesNotThrow(() -> sqsService.sendToDelivery(mandateEntity, EventType.MANDATE_ACCEPTED));
    }

    @Test
    void testSendToDeliveryAfterUpdate() {
        SqsClient amazonSQS = mock(SqsClient.class);
        GetQueueUrlResponse getQueueUrlResponse = GetQueueUrlResponse.builder().queueUrl("queueUrl").build();

        when(amazonSQS.getQueueUrl((GetQueueUrlRequest) any())).thenReturn(getQueueUrlResponse);

        SqsService sqsService = new SqsService("queueNameTest", amazonSQS, new ObjectMapper());

        assertDoesNotThrow(() -> sqsService.sendToDelivery(newMandateWithGroups(), newMandateWithGroups(), EventType.MANDATE_UPDATED));
    }

    public static MandateEntity newMandateWithGroups() {
        MandateEntity m = new MandateEntity();
        m.setMandateId("f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1");
        m.setDelegatorUid("f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegator("PF-f271e4bf-0d69-4ed6-a39f-4efdelegator");
        m.setDelegate("PF-f271e4bf-0d69-4ed6-a39f-4ef2delegate");
        m.setDelegatorisperson(true);
        m.setDelegateisperson(true);
        m.setGroups(new HashSet<>(List.of("f271e4bf-0d69-4ed6-a39f", "f271e4bf-0d69-4ed6-a50f")));
        m.setState(StatusEnumMapper.intValfromStatus(MandateDto.StatusEnum.PENDING));
        m.setValidationcode("12345");
        m.setVisibilityIds(null);
        return m;
    }

}

