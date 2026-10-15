package it.pagopa.pn.papertracker.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LogUtilityTest {

    private LogUtility logUtility;
    private ObjectMapper objectMapper;

    private static final String REQUEST_ID = "TEST";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        logUtility = new LogUtility(objectMapper);
    }

    //  POJO helper classes
    static class Payload {
        public AnalogMail analogMail;
    }

    static class AnalogMail {
        public String requestId;
        public DiscoveredAddress discoveredAddress;
    }

    static class DiscoveredAddress {
        public String name;
        public String nameRow2;
        public String address;
        public String addressRow2;
        public String cap;
        public String city;
        public String city2;
        public String pr;
        public String country;
    }

    static class User {
        public String name;
        public String fiscalCode;
        public Contacts contacts;
    }

    static class Contacts {
        public String email;
        public String phone;
    }

    static class Item {
        public int id;
        public DiscoveredAddress discoveredAddress;
    }

    static class ContainerWithItems {
        public List<Item> items;
    }


    @Test
    void shouldMaskDiscoveredAddressAtAnyDepth() throws Exception {
        // Arrange
        DiscoveredAddress address = new DiscoveredAddress();
        address.name = "Mario Rossi";
        address.address = "Via Roma 123";

        AnalogMail analogMail = new AnalogMail();
        analogMail.requestId = REQUEST_ID;
        analogMail.discoveredAddress = address;

        Payload payload = new Payload();
        payload.analogMail = analogMail;

        // Act
        String maskedJson = logUtility.maskSensitiveData(payload, "discoveredAddress");
        JsonNode node = objectMapper.readTree(maskedJson);

        // Assert
        assertThat(node.at("/analogMail/discoveredAddress").asText()).isEqualTo("***");
        assertThat(node.at("/analogMail/requestId").asText()).isEqualTo(REQUEST_ID);
    }

    @Test
    void shouldMaskObjectsInsideArrays() throws Exception {
        // Arrange
        Item item1 = new Item();
        item1.id = 1;
        item1.discoveredAddress = new DiscoveredAddress();
        item1.discoveredAddress.address = "Via Roma 1";

        Item item2 = new Item();
        item2.id = 2;
        item2.discoveredAddress = new DiscoveredAddress();
        item2.discoveredAddress.address = "Via Milano 2";

        ContainerWithItems container = new ContainerWithItems();
        container.items = List.of(item1, item2);

        // Act
        String maskedJson = logUtility.maskSensitiveData(container, "discoveredAddress");
        JsonNode root = objectMapper.readTree(maskedJson);

        // Assert
        for (JsonNode item : root.at("/items")) {
            assertThat(item.get("discoveredAddress").asText()).isEqualTo("***");
        }
    }

    @Test
    void shouldMaskMultipleSensitiveFields() throws Exception {
        // Arrange
        Contacts contacts = new Contacts();
        contacts.email = "mario.rossi@test.it";
        contacts.phone = "3331234567";

        User user = new User();
        user.name = "Mario Rossi";
        user.fiscalCode = "RSSMRA80A01H501U";
        user.contacts = contacts;

        // Act
        String maskedJson = logUtility.maskSensitiveData(
                user,
                "fiscalCode",
                "email",
                "phone"
        );
        JsonNode root = objectMapper.readTree(maskedJson);

        // Assert
        assertThat(root.at("/fiscalCode").asText()).isEqualTo("***");
        assertThat(root.at("/contacts/email").asText()).isEqualTo("***");
        assertThat(root.at("/contacts/phone").asText()).isEqualTo("***");
        assertThat(root.at("/name").asText()).isEqualTo("Mario Rossi");
    }

    @Test
    void shouldHandleNullInputSafely() {
        // Arrange
        Object obj = null;

        // Act
        String masked = logUtility.maskSensitiveData(obj, "anyField");

        // Assert
        assertThat(masked).isEqualTo("null");
    }

    @Test
    void shouldNotFailIfFieldIsAbsent() throws Exception {
        // Arrange
        Map<String, String> map = Map.of("foo", "bar");

        // Act
        String maskedJson = logUtility.maskSensitiveData(map, "discoveredAddress");
        JsonNode node = objectMapper.readTree(maskedJson);

        // Assert
        assertThat(node.get("foo").asText()).isEqualTo("bar");
    }

    @Test
    void shouldAnonymizePayloadAndExcludeBodyFromHeaders() {
        // Arrange
        DiscoveredAddress address = new DiscoveredAddress();
        address.name = "Mario Rossi";
        address.address = "Via Roma 123";

        AnalogMail analogMail = new AnalogMail();
        analogMail.requestId = REQUEST_ID;
        analogMail.discoveredAddress = address;

        Payload payload = new Payload();
        payload.analogMail = analogMail;

        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("SomeHeader", "SomeValue");
        headers.put("Body", "{\"analogMail\":{}}"); // Simula header Body

        Message<Payload> message = new GenericMessage<>(payload, new MessageHeaders(headers));

        // Act
        String result = logUtility.messageToString(message, Set.of("discoveredAddress"));

        // Assert
        // Il payload deve essere mascherato
        assertThat(result).contains("\"discoveredAddress\":\"***\"");
        // Il campo non sensibile resta invariato
        assertThat(result).contains(REQUEST_ID);
        // L'header "Body" non deve essere presente
        assertThat(result).doesNotContain(SqsHeaders.SQS_SOURCE_DATA_HEADER);
        // Gli altri header devono essere presenti
        assertThat(result).contains("SomeHeader=SomeValue");

        System.out.println(result);
    }
}