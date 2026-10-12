package it.pagopa.pn.papertracker.it._890;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.it.SequenceRunner;
import it.pagopa.pn.papertracker.it.model.ProductTestCase;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

import static it.pagopa.pn.papertracker.model.OcrStatusEnum.DRY;
import static it.pagopa.pn.papertracker.model.OcrStatusEnum.RUN;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
@TestPropertySource(
        locations = "classpath:application.test-IT.properties",
        properties = {
                "pn.paper-tracker.enable-ocr-validation-for=1970-01-01;AR:DRY;890:DRY"
        }
)
public class Dry890OcrDryTestIT extends Abstract890TestIT {

    @Autowired
    private SequenceRunner scenarioRunner;

    @ParameterizedTest(name = "{0}")
    @MethodSource("loadTestCases")
    void runScenario(String fileName, ProductTestCase scenario) throws InterruptedException {
        try {
            ArgumentCaptor<OcrEvent> ocrEventCaptor = ArgumentCaptor.forClass(OcrEvent.class);
            mockData(scenario, RUN);
            scenarioRunner.run(scenario, DRY,false);
            verifySentToOcr(scenario, ocrEventCaptor);
        }catch (PnPaperTrackerValidationException e){
            //se all'arrivo dell'evento C non sono presenti tutti gli statusCode necessari viene fatta salire l'eccezione
            //per consentire il riaccodamento del messaggio e il successivo reprocess degli eventi,
            // in questo modo si simula il comportamento del sistema in caso di eventi arrivati in ordine non corretto
            if(!e.getError().getDetails().getMessage().startsWith("Necessary status code not found in events:")){
                throw e;
            }
        }
    }

    Stream<Arguments> loadTestCases() throws Exception {
        return super.loadTestCases("_890", "/giacenza/multiple_ocr_request/run");
    }
}

