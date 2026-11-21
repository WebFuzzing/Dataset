package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CIEValidationData;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationRequest;
import it.pagopa.pn.mandate.appio.generated.openapi.server.v1.dto.MandateCreationResponse;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MandateAppIoControllerTest {

    private MandateService mandateService;
    private MandateAppIoController controller;

    @BeforeEach
    void setUp() {
        mandateService = mock(MandateService.class);
        controller = new MandateAppIoController(mandateService);
    }

    @Test
    void testCreateIOMandate() {
        String cxId = "testCxId";
        String lollipopUserName = "John";
        String lollipopUserFamilyName = "Doe";
        CxTypeAuthFleet cxType = CxTypeAuthFleet.PA;
        MandateCreationRequest request = new MandateCreationRequest();
        MandateCreationResponse response = new MandateCreationResponse();

        when(mandateService.createMandateAppIo(anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<MandateCreationResponse>> result = controller.createIOMandate(
                cxId, cxType, lollipopUserName, lollipopUserFamilyName, Mono.just(request), mock(ServerWebExchange.class)
        );

        StepVerifier.create(result)
                .expectNextMatches(entity -> entity.getStatusCode().is2xxSuccessful() && entity.getBody() == response)
                .verifyComplete();
    }


    @Test
    void testAcceptIOMandate() {
        String cxId = "testCxId";
        CxTypeAuthFleet cxType = CxTypeAuthFleet.PA;
        String mandateId = "mandateId";
        CIEValidationData cieValidationData = new CIEValidationData();

        when(mandateService.acceptMandateAppIo(anyString(), any(), anyString(), any()))
                .thenReturn(Mono.empty());

        Mono<ResponseEntity<Void>> result = controller.acceptIOMandate(
                cxId, cxType, mandateId, Mono.just(cieValidationData), mock(ServerWebExchange.class)
        );

        StepVerifier.create(result)
                .expectNextMatches(entity -> entity.getStatusCode().is2xxSuccessful() && entity.getStatusCodeValue() == 204)
                .verifyComplete();

        verify(mandateService).acceptMandateAppIo(eq(cxId), eq(cxType), eq(mandateId), any());
    }
}
