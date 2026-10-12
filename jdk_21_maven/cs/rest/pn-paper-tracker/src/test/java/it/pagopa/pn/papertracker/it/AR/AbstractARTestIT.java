package it.pagopa.pn.papertracker.it.AR;

import com.sngular.apigenerator.asyncapi.business_model.model.event.DataDTO;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataPayloadDTO;
import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import it.pagopa.pn.papertracker.it.SequenceLoader;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.OcrMomProducer;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static it.pagopa.pn.papertracker.it.GenericTestCaseHandlerImpl.getPcRetryResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractARTestIT extends BaseTest.WithLocalStack {

    private static final Set<String> SINGLE_PC_RETRY_SCENARIOS = Set.of(
            "OK_RETRY_AR",
            "OK_RETRY_AR_2",
            "OK_RETRY_M10_AR",
            "OK_CAUSA_FORZA_MAGGIORE_AR",
            "OK_NON_RENDICONTABILE_AR"
    );

    private static final Set<String> DOUBLE_PC_RETRY_SCENARIOS = Set.of(
            "FAIL_CON996_PC_RETRY_FURTO_AR"
    );

    @MockitoBean
    protected PaperChannelClient paperChannelClient;

    @MockitoBean
    protected SafeStorageClient safeStorageClient;

    @MockitoBean
    protected OcrMomProducer ocrMomProducer;

    @MockitoBean
    protected DataVaultClient dataVaultClient;

    protected void mockData(ProductTestCase productTestCase, OcrStatusEnum ocrStatusEnum){
        mockPcRetry(productTestCase);
        mockDataVault(productTestCase);
        if(!ocrStatusEnum.equals(OcrStatusEnum.DISABLED)) {
            mockSendToOcr(productTestCase);
        }
    }

    protected void mockDataVault(ProductTestCase scenario) {
        if (scenario.getEvents().stream().anyMatch(testEvent -> Objects.nonNull(testEvent.getAnalogMail()) && Objects.nonNull(testEvent.getAnalogMail().getDiscoveredAddress()))) {
            Mockito.when(dataVaultClient.anonymizeDiscoveredAddress(any(), any())).thenReturn(Mono.just("anonymized_addr_093bcc30-106e-4218-870c-aa9e1d56c4b9"));
            PaperAddress paperAddress = PaperAddress.builder()
                    .name("test")
                    .address("via Roma 10")
                    .cap("00100")
                    .pr("RM")
                    .city("Roma")
                    .build();
            Mockito.when(dataVaultClient.deAnonymizeDiscoveredAddress(any(), any())).thenReturn(Mono.just(paperAddress));
        }
    }

    protected void mockPcRetry(ProductTestCase scenario) {

        getPcRetryResponse(scenario);
        String scenarioName = scenario.getName().toUpperCase();

        if (SINGLE_PC_RETRY_SCENARIOS.contains(scenarioName)) {
            Mockito.when(paperChannelClient.getPcRetry(any(), any()))
                    .thenReturn(Mono.just(scenario.getFirstPcRetryResponse()));
        }

        if (DOUBLE_PC_RETRY_SCENARIOS.contains(scenarioName)) {
            Mockito.when(paperChannelClient.getPcRetry(any(), any()))
                    .thenReturn(Mono.just(scenario.getFirstPcRetryResponse()))
                    .thenReturn(Mono.just(scenario.getSecondPcRetryResponse()));
        }
    }

    protected void mockSendToOcr(ProductTestCase scenario) {
        if (Objects.nonNull(scenario.getExpected().getOcrDataPayload())) {
            Mockito.when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("Uri"));
            Mockito.doNothing().when(ocrMomProducer).push(any(OcrEvent.class));
        }
    }


    protected void verifySentToOcr(ProductTestCase scenario, ArgumentCaptor<OcrEvent> ocrEventCaptor){
        Mockito.verify(ocrMomProducer, times(scenario.getExpected().getSentToOcr())).push(ocrEventCaptor.capture());
        List<OcrDataPayloadDTO> ocrEvents = ocrEventCaptor.getAllValues().stream().map(OcrEvent::getPayload).toList();
        List<OcrDataPayloadDTO> expected = scenario.getExpected().getOcrDataPayload();
        if(Objects.nonNull(expected)) {
            Assertions.assertEquals(expected.size(), ocrEvents.size());
            for (int i = 0; i < expected.size(); i++) {
                OcrDataPayloadDTO exp = expected.get(i);
                OcrDataPayloadDTO act = ocrEvents.get(i);
                Assertions.assertEquals(exp.getCommandId(), act.getCommandId());
                Assertions.assertEquals(exp.getCommandType(), act.getCommandType());
                Assertions.assertNotNull(act.getData());
                DataDTO expData = exp.getData();
                DataDTO actData = act.getData();
                Assertions.assertEquals(expData.getDocumentType(), actData.getDocumentType());
                Assertions.assertEquals(expData.getProductType(), actData.getProductType());
                Assertions.assertEquals(expData.getUnifiedDeliveryDriver(), actData.getUnifiedDeliveryDriver());
                Assertions.assertNotNull(actData.getDetails());
                Assertions.assertEquals(expData.getDetails().getDeliveryDetailCode(), actData.getDetails().getDeliveryDetailCode());
                Assertions.assertEquals(expData.getDetails().getRegisteredLetterCode(), actData.getDetails().getRegisteredLetterCode());
                Assertions.assertEquals(expData.getDetails().getAttachment(), actData.getDetails().getAttachment());
                Assertions.assertEquals(expData.getDetails().getNotificationDate(), actData.getDetails().getNotificationDate());
                Assertions.assertEquals(expData.getDetails().getDeliveryFailureCause(), actData.getDetails().getDeliveryFailureCause());
                Assertions.assertEquals(expData.getDetails().getDeliveryAttemptDate(), actData.getDetails().getDeliveryAttemptDate());
            }
        }
    }


    protected Stream<Arguments> loadTestCases(String folder, String exclude, boolean ocrDisable) throws Exception {
        ClassPathResource classPathResource = new ClassPathResource("testcase/" + folder);
        URI excludeURI = null;
        if (StringUtils.hasText(exclude)) {
            ClassPathResource classPathResourceExclude = new ClassPathResource("testcase/" + folder + exclude);
            excludeURI = classPathResourceExclude.getURI();
        }

        Stream<Arguments> scenarios = SequenceLoader.loadScenarios(classPathResource.getURI(), excludeURI);

        if(ocrDisable) {
            ClassPathResource extraResource = new ClassPathResource("testcase/rework/ok_ar_rework.json");
            Stream<Arguments> extraScenarios = SequenceLoader.loadScenarios(extraResource.getURI(), null);
            return Stream.concat(scenarios, extraScenarios);
        }

        return scenarios;
    }

}