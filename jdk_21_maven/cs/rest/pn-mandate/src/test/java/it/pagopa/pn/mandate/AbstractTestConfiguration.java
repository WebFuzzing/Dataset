package it.pagopa.pn.mandate;

import it.pagopa.pn.ciechecker.CieChecker;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
public class AbstractTestConfiguration {
    /**
     * Sostituisce l'interfaccia CieChecker con un mock di Mockito,
     * rimuovendo le implementazioni concrete (es. cieCheckerImpl) dal contesto
     * per tutti i test che estendono questa classe.
     */
    @MockitoBean
    protected CieChecker cieCheckerMock;
}
