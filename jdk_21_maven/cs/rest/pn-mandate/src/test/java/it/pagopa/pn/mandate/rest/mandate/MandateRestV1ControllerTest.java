package it.pagopa.pn.mandate.rest.mandate;

import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.mandate.exceptions.PnForbiddenException;
import it.pagopa.pn.mandate.exceptions.PnMandateNotFoundException;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateCountsDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.SearchMandateResponseDto;
import it.pagopa.pn.mandate.mapper.MandateEntityMandateDtoMapper;
import it.pagopa.pn.mandate.mapper.UserEntityMandateCountsDtoMapper;
import it.pagopa.pn.mandate.middleware.db.MandateDaoIT;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.msclient.PnExtRegPrvtClient;
import it.pagopa.pn.mandate.services.mandate.utils.MandateValidationUtils;
import it.pagopa.pn.mandate.services.mandate.v1.MandateService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

@WebFluxTest(controllers = {MandateRestV1Controller.class})
@Import({MandateEntityMandateDtoMapper.class, UserEntityMandateCountsDtoMapper.class, MandateValidationUtils.class })
class MandateRestV1ControllerTest {

    public static final String PN_PAGOPA_USER_ID = "x-pagopa-pn-uid";
    public static final String PN_PAGOPA_CX_TYPE = "x-pagopa-pn-cx-type";
    public static final String PN_PAGOPA_CX_ID = "x-pagopa-pn-cx-id";
    public static final String PN_PAGOPA_CX_ROLE = "x-pagopa-pn-cx-role";
    public static final String PN_PAGOPA_CX_SRC_CH = "x-pagopa-pn-src-ch";

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private MandateEntityMandateDtoMapper mapper;

    @Autowired
    private MandateValidationUtils validationUtils;

    @MockitoBean
    private MandateService mandateService;

    @MockitoBean
    private ValidateUtils validateUtils;

    @MockitoBean
    private PnExtRegPrvtClient pnExtRegPrvtClient;

    @Test
    void countMandatesByDelegate() {
        //Given
        String url = "/mandate/api/v1/count-by-delegate";
        MandateCountsDto mandateCount = new MandateCountsDto();
        mandateCount.setValue(5);

        //When
        Mockito.when(mandateService.countMandatesByDelegate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(),Mockito.any()))
                .thenReturn(Mono.just(mandateCount));

        //Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void acceptMandate() {
        //Given
        String url = "/mandate/api/v1/mandate/{mandateId}/accept"
                .replace("{mandateId}", "123e4567-e89b-12d3-a456-426614174000");

        //When
        Mockito.when( mandateService.acceptMandate( Mockito.any(), Mockito.any() , Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(new MandateEntity()));


        //Then
        webTestClient.patch()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header( PN_PAGOPA_CX_ID, "internaluserid1234")
                .header( PN_PAGOPA_USER_ID, "userid")
                .header( PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void updateMandate() {
        //Given
        String url = "/mandate/api/v1/mandate/{mandateId}/update"
                .replace("{mandateId}", "123e4567-e89b-12d3-a456-426614174000");

        //When
        Mockito.when( mandateService.updateMandate( Mockito.any(), Mockito.any() , Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        //Then
        webTestClient.patch()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header( PN_PAGOPA_CX_ID, "internaluserid1234")
                .header( PN_PAGOPA_USER_ID, "userid")
                .header( PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isNoContent();
    }


    @Test
    void createMandate() {
        //Given
        String url = "/mandate/api/v1/mandate";

        MandateDto dto = mapper.toDto(MandateDaoIT.newMandate(true));

        //When
        Mockito.when(mandateService.createMandate(Mockito.any(), Mockito.any() , Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(dto));

        //Then
        webTestClient.post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_SRC_CH, "IO")
                .exchange()
                .expectStatus().isCreated()
                .expectBody();
    }

    @Test
    void listMandatesByDelegate1() {
        //Given
        String url = "/mandate/api/v1/mandates-by-delegate";
        List<MandateDto> mandateDtoList = Collections.singletonList(mapper.toDto(MandateDaoIT.newMandate(true)));

        //When
        Mockito.when(mandateService.listMandatesByDelegate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(mandateDtoList));

        //Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isOk().expectBodyList(MandateDto.class);
    }


    @Test
    void listMandatesByDelegate1Validation() {
        assertDoesNotThrow(()->
            validationUtils.validateListMandatesByDelegateRequest( CxTypeAuthFleet.PF)
        );

        Throwable exception = assertThrows(PnForbiddenException.class, () -> validationUtils.validateListMandatesByDelegateRequest( CxTypeAuthFleet.PG));
        assertEquals("Accesso negato!", exception.getMessage());

    }

    @Test
    void listMandatesByDelegator1() {
        //Given
        String url = "/mandate/api/v1/mandates-by-delegator";
        List<MandateDto> mandateDtoList = Collections.singletonList(mapper.toDto(MandateDaoIT.newMandate(true)));

        //When
        Mockito.when(mandateService.listMandatesByDelegator(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Flux.fromIterable(mandateDtoList));

        //Then
        webTestClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isOk().expectBodyList(MandateDto.class);
    }

    @Test
    void rejectMandate() {
        //Given
        String url = "/mandate/api/v1/mandate/{mandateId}/reject"
                .replace("{mandateId}", "123e4567-e89b-12d3-a456-426614174000");

        //When
        Mockito.when(mandateService.rejectMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.empty());

        //Then
        webTestClient.patch()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isNoContent();
    }


    @Test
    void rejectMandate_fail() {
        //Given
        String url = "/mandate/api/v1/mandate/{mandateId}/reject"
                .replace("{mandateId}", "123e4567-e89b-12d3-a456-426614174000");

        //When
        Mockito.when(mandateService.rejectMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new PnMandateNotFoundException()));

        //Then
        webTestClient.patch()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().is4xxClientError();
    }


    @Test
    void rejectMandate_fail1() {
        //Given
        String url = "/mandate/api/v1/mandate/{mandateId}/reject"
                .replace("{mandateId}", "123e4567-e89b-12d3-a456-426614174000");

        //When
        Mockito.when(mandateService.rejectMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.error(new PnMandateNotFoundException()));

        //Then
        webTestClient.patch()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void revokeMandate() {
        //Given
        String url = "/mandate/api/v1/mandate/{mandateId}/revoke"
                .replace("{mandateId}", "123e4567-e89b-12d3-a456-426614174000");

        //When
        Mockito.when(mandateService.revokeMandate(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(Mono.just(""));

        //Then
        webTestClient.patch()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "internaluserid1234")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PF")
                .exchange()
                .expectStatus().isNoContent();
    }

    @Test
    void searchMandatesByDelegate() {
        //Given
        String url = "/mandate/api/v1/mandates-by-delegate?size={size}"
                .replace("{size}", "1");

        SearchMandateResponseDto responseDto = new SearchMandateResponseDto();

        //When
        Mockito.when(mandateService.searchByDelegate(Mockito.any(), eq(1), isNull(), eq("cx-id"), eq(CxTypeAuthFleet.PG), isNull(), eq("admin")))
                .thenReturn(Mono.just(responseDto));

        //Then
        webTestClient.post()
                .uri(url)
                .accept(MediaType.APPLICATION_JSON)
                .header(PN_PAGOPA_CX_ID, "cx-id")
                .header(PN_PAGOPA_USER_ID, "userid")
                .header(PN_PAGOPA_CX_TYPE, "PG")
                .header(PN_PAGOPA_CX_ROLE, "admin")
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SearchMandateResponseDto.class);
    }
}