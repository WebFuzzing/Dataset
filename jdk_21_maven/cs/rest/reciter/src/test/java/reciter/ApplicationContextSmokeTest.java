package reciter;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import reciter.consumer.service.CognitoAuthService;
import reciter.database.dynamodb.DynamoDbConfig;
import reciter.security.CognitoClientRegistry;
import reciter.service.GenderService;
import reciter.service.NameFrequencyService;
import reciter.service.ScienceMetrixDepartmentCategoryService;
import reciter.service.ScienceMetrixService;
import reciter.service.dynamo.DynamoDbInstitutionAfidService;
import reciter.service.dynamo.DynamoDbMeshTermService;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Stage 0 context-load smoke test (#634 dependency modernization).
 *
 * <p>Boots the full Spring application context under the dedicated {@code test}
 * profile (see {@code application-test.properties}), which neutralizes the
 * property-controlled live-AWS startup blockers. The {@code @MockitoBean} overrides
 * below stand in for the beans that build live AWS SDK clients or run data loads
 * at startup:
 * <ul>
 *   <li>{@code AmazonDynamoDB} / {@code AmazonS3} — would otherwise hit DynamoDB/S3/STS.</li>
 *   <li>{@code CognitoAuthService} / {@code CognitoClientRegistry} — build live Cognito clients.</li>
 *   <li>The six services invoked by {@code Application}'s {@code ApplicationReadyEvent}
 *       listeners — mocked so the startup data-load is a no-op (Mockito returns empty
 *       lists for list-returning methods by default).</li>
 * </ul>
 *
 * <p>This is the per-stage gate for #634: every framework/wiring change (Stages 1-3)
 * must keep this green, proving the context still wires end to end. Stage 2 also
 * asserts that springdoc (which replaced springfox) actually generates the OpenAPI
 * document for the {@code reciter} controller group.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class ApplicationContextSmokeTest {

	@MockitoBean
    private DynamoDbClient dynamoDbClient;

	@MockitoBean
    private S3Client s3Client;
    
	@MockitoBean
    private CognitoAuthService cognitoAuthService;

	@MockitoBean
    private CognitoClientRegistry cognitoClientRegistry;

    // Services invoked by Application's ApplicationReadyEvent listeners. Mocking them
    // makes populateStaticEngineParameters() a no-op at startup without needing AWS.
	@MockitoBean
    private ScienceMetrixService scienceMetrixService;

	@MockitoBean
    private ScienceMetrixDepartmentCategoryService scienceMetrixDepartmentCategoryService;

	@MockitoBean
    private DynamoDbMeshTermService dynamoDbMeshTermService;

	@MockitoBean
    private GenderService genderService;

	@MockitoBean
    private NameFrequencyService nameFrequencyService;

	@MockitoBean
    private DynamoDbInstitutionAfidService dynamoDbInstitutionAfidService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void contextLoads() {
        assertNotNull("Spring application context should load", context);
        // A concrete bean from the DynamoDB wiring path proves the persistence config
        // wired (the surface Stage 1 will rework when spring-data-dynamodb is removed).
        assertNotNull("DynamoDbConfig should be wired", context.getBean(DynamoDbConfig.class));
    }

    /**
     * Stage 2: springdoc generates the OpenAPI document for the {@code reciter} group
     * (GroupedOpenApi scoped to reciter.controller / {@code /reciter/**}). Proves the
     * springfox-&gt;springdoc migration and the v1-&gt;v3 annotation conversion actually
     * produce docs, not just that the context loads.
     */
    @Test
    public void springdocApiDocsAreGenerated() throws Exception {
        // springdoc.api-docs.path is /reciter/v3/api-docs, so the "reciter" group document
        // is served at {api-docs.path}/{group}, i.e. the group name is reciter-group.
        mockMvc.perform(get("/reciter/v3/api-docs/reciter-group"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"openapi\"")))
                .andExpect(content().string(containsString("/reciter/ping")))
                .andExpect(content().string(containsString("ReCiter publication management system")));
    }
}
