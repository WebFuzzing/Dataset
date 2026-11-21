package it.pagopa.pn.mandate;

import io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@EnableAutoConfiguration(exclude = {SqsAutoConfiguration.class})
public class MockAWSObjectsTestConfig {
    @MockitoBean
    private SqsAsyncClient sqsAsyncClient;
}
