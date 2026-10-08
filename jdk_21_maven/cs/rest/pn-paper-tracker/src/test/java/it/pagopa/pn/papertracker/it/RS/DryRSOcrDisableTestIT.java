package it.pagopa.pn.papertracker.it.RS;

import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.it.SequenceLoader;
import it.pagopa.pn.papertracker.it.SequenceRunner;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;
import java.util.stream.Stream;

import static it.pagopa.pn.papertracker.it.GenericTestCaseHandlerImpl.getPcRetryResponse;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application.test-IT.properties",
        properties = {
                "pn.paper-tracker.enable-ocr-validation-for=1970-01-01;"
        }
)
public class DryRSOcrDisableTestIT extends BaseTest.WithLocalStack {

    @Autowired
    private SequenceRunner scenarioRunner;

    @MockitoBean
    private PaperChannelClient paperChannelClient;

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadTestCases")
    void runScenario(String fileName, ProductTestCase scenario) throws InterruptedException {
        try {
            mockPcRetry(scenario);
            scenarioRunner.run(scenario, OcrStatusEnum.DISABLED, false);
        }catch (PnPaperTrackerValidationException e){
            //se all'arrivo dell'evento C non sono presenti tutti gli statusCode necessari viene fatta salire l'eccezione
            //per consentire il riaccodamento del messaggio e il successivo reprocess degli eventi,
            // in questo modo si simula il comportamento del sistema in caso di eventi arrivati in ordine non corretto
            if(!e.getError().getDetails().getMessage().equalsIgnoreCase("Necessary status code not found in events: [RECRS004B]")){
                throw e;
            }
        }
    }

    private void mockPcRetry(ProductTestCase scenario) {
        getPcRetryResponse(scenario);
        if (scenario.getName().equalsIgnoreCase("OK_RETRY_RS")
                || scenario.getName().equalsIgnoreCase("OK_RETRY_M10_RS")
                || scenario.getName().equalsIgnoreCase("OK_NON_RENDICONTABILE_RS")) {
            Mockito.when(paperChannelClient.getPcRetry(any(), any())).thenReturn(Mono.just(scenario.getFirstPcRetryResponse()));
        }
    }

    Stream<Arguments> loadTestCases() throws Exception {
        URI uri = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader()
                .getResource("testcase/RS")).toURI();

       return SequenceLoader.loadScenarios(uri, null);
    }
}
