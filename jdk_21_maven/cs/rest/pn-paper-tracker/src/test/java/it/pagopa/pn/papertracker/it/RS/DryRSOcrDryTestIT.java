package it.pagopa.pn.papertracker.it.RS;

import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.it.SequenceLoader;
import it.pagopa.pn.papertracker.it.SequenceRunner;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
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
import static it.pagopa.pn.papertracker.model.OcrStatusEnum.DRY;
import static org.mockito.ArgumentMatchers.any;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application.test-IT.properties",
        properties = {
                "pn.paper-tracker.enable-ocr-validation-for=1970-01-01;AR:DRY;890:DRY;RS:DRY"
        }
)
public class DryRSOcrDryTestIT extends AbstractRSTestIT {

    @Autowired
    private SequenceRunner scenarioRunner;

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadTestCases")
    void runScenario(String fileName, ProductTestCase scenario) throws InterruptedException {
        try {
            ArgumentCaptor<OcrEvent> ocrEventCaptor = ArgumentCaptor.forClass(OcrEvent.class);
            mockData(scenario, DRY);
            scenarioRunner.run(scenario, DRY,false);
            verifySentToOcr(scenario, ocrEventCaptor);
        }catch (PnPaperTrackerValidationException e){
            //se all'arrivo dell'evento C non sono presenti tutti gli statusCode necessari viene fatta salire l'eccezione
            //per consentire il riaccodamento del messaggio e il successivo reprocess degli eventi,
            // in questo modo si simula il comportamento del sistema in caso di eventi arrivati in ordine non corretto
            if(!e.getError().getDetails().getMessage().equalsIgnoreCase("Necessary status code not found in events: [RECRS004B]")){
                throw e;
            }
        }
    }

    Stream<Arguments> loadTestCases() throws Exception {
        URI uri = Objects.requireNonNull(Thread.currentThread()
                .getContextClassLoader()
                .getResource("testcase/RS")).toURI();

       return SequenceLoader.loadScenarios(uri, null);
    }
}
