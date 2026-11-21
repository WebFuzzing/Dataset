package it.pagopa.pn.mandate.middleware.msclient;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.exceptions.PnInvalidQrCodeException;
import it.pagopa.pn.mandate.generated.openapi.msclient.delivery.v1.dto.UserInfoQrCodeDto;
import org.junit.jupiter.api.*;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@ActiveProfiles("test")
@TestPropertySource(properties = {
        "pn.mandate.client_delivery_basepath=http://localhost:9999"
})
class PnDeliveryClientTest extends AbstractTestConfiguration {

    @Autowired
    private PnDeliveryClient client;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9999);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @AfterEach
    void resetMockServer() {
        mockServer.reset();
    }

    @Test
    void decodeAarQrCode_success() {
        // Given
        String token = "token";
        String expectedDenomination = "Mario Rossi";
        new org.mockserver.client.MockServerClient("localhost", 9999)
                .when(
                        org.mockserver.model.HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/delivery-private/notifications/qr-code/decode")
                                .withBody("{\"aarTokenValue\":\"" + token + "\"}")
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                                .withBody("{\"recipientInfo\":{\"denomination\":\"" + expectedDenomination + "\"}}")
                                .withStatusCode(200)
                                .withContentType(org.mockserver.model.MediaType.APPLICATION_JSON)
                );

        // When
        UserInfoQrCodeDto result = client.decodeAarQrCode(token).block(java.time.Duration.ofMillis(3000));

        // Then
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getRecipientInfo());
        Assertions.assertEquals(expectedDenomination, result.getRecipientInfo().getDenomination());

    }


    @Test
    void decodeAarQrCode_badRequest() {
        String token = "token";
        new org.mockserver.client.MockServerClient("localhost", 9999)
                .when(
                        org.mockserver.model.HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/delivery-private/notifications/qr-code/decode")
                                .withBody("{\"aarTokenValue\":\"" + token + "\"}")
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                                .withStatusCode(404)
                );

        Assertions.assertThrows(PnInvalidQrCodeException.class, () -> client.decodeAarQrCode(token).block(java.time.Duration.ofMillis(3000)));
    }

    @Test
    void decodeAarQrCode_internalError() {
        // Given
        String token = "token";
        new org.mockserver.client.MockServerClient("localhost", 9999)
                .when(
                        org.mockserver.model.HttpRequest.request()
                                .withMethod("POST")
                                .withPath("/delivery-private/notifications/qr-code/decode")
                                .withBody("{\"aarTokenValue\":\"" + token + "\"}")
                )
                .respond(
                        org.mockserver.model.HttpResponse.response()
                                .withStatusCode(500)
                );

        // When & Then
        Assertions.assertThrows(PnInternalException.class, () -> client.decodeAarQrCode(token).block(java.time.Duration.ofMillis(3000)));
    }


}
