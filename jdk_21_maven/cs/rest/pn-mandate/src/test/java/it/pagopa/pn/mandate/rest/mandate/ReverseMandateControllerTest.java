package it.pagopa.pn.mandate.rest.mandate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDtoRequest;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDtoResponse;
import it.pagopa.pn.mandate.mapper.ReverseMandateEntityMandateDtoMapper;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import it.pagopa.pn.mandate.utils.MandateUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

@WebFluxTest(controllers = {ReverseMandateController.class})
@Import({ReverseMandateEntityMandateDtoMapper.class, MandateUtils.class})
class ReverseMandateControllerTest {

    public static final String PN_PAGOPA_USER_ID = "x-pagopa-pn-uid";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_ROLE = "x-pagopa-pn-cx-role";
    public static final String PN_PAGOPA_CX_SRC_CH = "x-pagopa-pn-src-ch";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private MandateService mandateService;

    @Test
    void createMandateSuccessfully() throws JsonProcessingException {
        MandateDtoRequest request = new MandateDtoRequest();
        String requestBody = new ObjectMapper().writeValueAsString(request);
        Mockito.when(mandateService.createReverseMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just("mandate-id"));

        webTestClient.post()
                .uri("/mandate/api/v1/reverse-mandate")
                .header(PN_PAGOPA_USER_ID, "user-id")
                .header(PN_PAGOPA_CX_ID, "cx-id")
                .header(PN_PAGOPA_CX_TYPE, CxTypeAuthFleet.PG.toString())
                .header(PN_PAGOPA_CX_ROLE, "cx-role")
                .header(PN_PAGOPA_CX_SRC_CH, "IO")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(requestBody))
                .exchange()
                .expectStatus().isCreated()
                .expectBody(MandateDtoResponse.class);
    }
}