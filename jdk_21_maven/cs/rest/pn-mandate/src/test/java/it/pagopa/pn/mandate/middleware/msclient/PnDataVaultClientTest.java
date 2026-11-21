package it.pagopa.pn.mandate.middleware.msclient;

import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.BaseRecipientDtoDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.datavault.v1.dto.MandateDtoDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;


@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.mandate.client_datavault_basepath=http://localhost:9997"
})
class PnDataVaultClientTest extends AbstractTestConfiguration {

    @Autowired
    private PnDataVaultClient pnDataVaultClient;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9997);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void getRecipientDenominationByInternalId() {
        //Given
        String cf = "RSSMRA85T10A562S";
        String iuid= "abcd-123-fghi";
        String denominazione = "mario rossi";
        List<String> list = new ArrayList<>();
        list.add(iuid);

        new MockServerClient("localhost", 9997)
                .when(request()
                        .withMethod("GET")
                        .withQueryStringParameter("internalId", iuid)
                        .withPath("/datavault-private/v1/recipients/internal"))
                .respond(response()
                        .withBody("{" +
                                "\"" + "internalId" + "\": " + "\"" + iuid + "\"," +
                                "\"" + "denomination" + "\": " + "\"" + denominazione + "\"" +
                                "}")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        List<BaseRecipientDtoDto> result = pnDataVaultClient.getRecipientDenominationByInternalId(list).collectList().block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(iuid, result.get(0).getInternalId());
        assertEquals(denominazione, result.get(0).getDenomination());
    }

    @Test
    void ensureRecipientByExternalId() {
        //Given
        String cf = "RSSMRA85T10A562S";
        String iuid= "abcd-123-fghi";


        new MockServerClient("localhost", 9997)
                .when(request()
                        .withMethod("POST")
                        .withBody(cf)
                        .withPath("/datavault-private/v1/recipients/external/{recipientType}".replace("{recipientType}","PF")))
                .respond(response()
                        .withBody(iuid)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        String result = pnDataVaultClient.ensureRecipientByExternalId(true, cf).block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(iuid, result);
    }

    @Test
    void updateMandateById() {
        //Given
        String mandateid = "f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1";
        String name= "mario";
        String surname= "rossi";
        String ragionesociale= "mr srl";


        new MockServerClient("localhost", 9997)
                .when(request()
                        .withMethod("PUT")
                        .withPath("/datavault-private/v1/mandates/{mandateId}".replace("{mandateId}",mandateid)))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        String result = pnDataVaultClient.updateMandateById(mandateid, name, surname, ragionesociale).block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals("OK", result);
    }

    @Test
    void deleteMandateById() {
        //Given
        String mandateid = "f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1";


        new MockServerClient("localhost", 9997)
                .when(request()
                        .withMethod("DELETE")
                        .withPath("/datavault-private/v1/mandates/{mandateId}".replace("{mandateId}",mandateid)))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        assertDoesNotThrow(() -> pnDataVaultClient.deleteMandateById(mandateid).block(Duration.ofMillis(3000)));

        //Then
    }

    @Test
    void getMandatesByIds() {
        //Given
        String mandateid = "f271e4bf-0d69-4ed6-a39f-4ef2f01f2fd1";
        String name= "mario";
        String surname= "rossi";
        String ragionesociale= "mr srl";

        List<String> list = new ArrayList<>();
        list.add(mandateid);
        new MockServerClient("localhost", 9997)
                .when(request()
                        .withMethod("GET")
                        .withQueryStringParameter("mandateId", mandateid)
                        .withPath("/datavault-private/v1/mandates"))
                .respond(response()
                        .withBody("{" +
                                "\"" + "mandateId" + "\": " + "\"" + mandateid + "\"," +
                                "\"" + "info" + "\": " +
                                "{" + "\"" + "destName" + "\": " + "\"" + name + "\"," +
                                        "\"" + "destSurname" + "\": " + "\"" + surname + "\"," +
                                        "\"" + "destBusinessName" + "\": " + "\"" + ragionesociale + "\""
                                +"}" +
                                "}")
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

        //When
        List<MandateDtoDto> result = pnDataVaultClient.getMandatesByIds(list).collectList().block(Duration.ofMillis(3000));

        //Then
        assertNotNull(result);
        assertEquals(mandateid, result.get(0).getMandateId());
        assertNotNull(result.get(0).getInfo());
        assertEquals(name, result.get(0).getInfo().getDestName());
        assertEquals(surname, result.get(0).getInfo().getDestSurname());
        assertEquals(ragionesociale, result.get(0).getInfo().getDestBusinessName());
    }
}