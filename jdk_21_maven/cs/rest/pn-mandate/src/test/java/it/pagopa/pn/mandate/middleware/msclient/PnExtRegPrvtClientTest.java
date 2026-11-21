package it.pagopa.pn.mandate.middleware.msclient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.mandate.AbstractTestConfiguration;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcare.v1.dto.FilteredPaIdsResponseDto;
import it.pagopa.pn.mandate.generated.openapi.msclient.extregselfcaregroups.v1.dto.PgGroupDto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ActiveProfiles("test")
@TestPropertySource(properties = {
    "pn.mandate.client_extreg_basepath=http://localhost:9996"
})
class PnExtRegPrvtClientTest extends AbstractTestConfiguration {

    @Autowired
    private PnExtRegPrvtClient pnExtRegPrvtClient;

    private static ClientAndServer mockServer;

    @BeforeAll
    public static void startMockServer() {
        mockServer = startClientAndServer(9996);
    }

    @AfterAll
    public static void stopMockServer() {
        mockServer.stop();
    }

    @Test
    void getGroups() {
        // Given
        PgGroupDto pgGroupDto = new PgGroupDto();
        pgGroupDto.setId("id");
        pgGroupDto.setName("name");

        List<PgGroupDto> res = List.of(pgGroupDto);
        byte[] responseBodyBytes = new byte[0];

        ObjectMapper mapper = new ObjectMapper();
        try {
            responseBodyBytes = mapper.writeValueAsBytes(res);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        new MockServerClient("localhost", 9996)
                .when(request()
                        .withMethod("GET")
                        .withPath("/ext-registry-private/pg/v1/groups-all")
                        .withHeader("x-pagopa-pn-cx-id", "cx-id"))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(responseBodyBytes)
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withStatusCode(200));

            // When
            List<PgGroupDto> result = pnExtRegPrvtClient.getGroups("cx-id", false)
                    .collectList()
                    .block(Duration.ofMillis(3000));

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("id", result.get(0).getId());
            assertEquals("name", result.get(0).getName());
    }

    @Test
    void checkAooUoV2Ids_integration() throws Exception {
        // Given
        List<String> input = Arrays.asList("id1", "id2", "id3");
        List<String> filtered = Arrays.asList("id2", "id3");
        ObjectMapper mapper = new ObjectMapper();

        FilteredPaIdsResponseDto responseDto = new FilteredPaIdsResponseDto();
        responseDto.setIds(filtered);

        byte[] responseBodyBytes = mapper.writeValueAsBytes(responseDto);

        new MockServerClient("localhost", 9996)
                .when(request()
                        .withMethod("GET")
                        .withPath("/ext-registry-private/pa/v2/actions/filter-out-root-pa-ids")
                        .withQueryStringParameter("id", "id1")
                        .withQueryStringParameter("id", "id2")
                        .withQueryStringParameter("id", "id3"))
                .respond(response()
                        .withContentType(MediaType.APPLICATION_JSON)
                        .withBody(responseBodyBytes)
                        .withStatusCode(200));

        // When
        FilteredPaIdsResponseDto result = pnExtRegPrvtClient.checkAooUoV2Ids(input)
                .block(Duration.ofMillis(3000));

        // Then
        assertNotNull(result);
        assertNotNull(result.getIds());
        assertEquals(2, result.getIds().size());
        assertEquals("id2", result.getIds().get(0));
        assertEquals("id3", result.getIds().get(1));
    }


}