package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.InternalMandateDto;
import it.pagopa.pn.mandate.mapper.MandateEntityInternalMandateDtoMapper;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.services.mandate.v1.MandatePrivateService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;

@WebFluxTest(controllers = {MandatePrivateRestV2Controller.class})
@Import(MandateEntityInternalMandateDtoMapper.class)
class MandatePrivateRestV2ControllerTest {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    MandateEntityInternalMandateDtoMapper mapper;

    @MockitoBean
    private MandatePrivateService mandatePrivateService;

    @Test
    void listMandatesByDelegate() {
        //Given
        String url = "/mandate-private/api/v2/mandates-by-internaldelegate/{internaluserId}?x-pagopa-pn-cx-type={type}"
                .replace("{internaluserId}", "internauserid1234")
                .replace("{type}", "PF");
        List<InternalMandateDto> mandateDtoList = Collections.singletonList(mapper.toDto(MandateDaoIT.newMandate(true)));

        //When
        Mockito.when(mandatePrivateService.listMandatesByDelegateV2(Mockito.any()))
                .thenReturn(Flux.fromIterable(mandateDtoList));

        //Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();
    }

}