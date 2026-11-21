package reciter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import reciter.consumer.service.CognitoAuthService;
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
 * Stage 3 (#634): exercises the migrated {@code SecurityFilterChain} + {@code WebSecurityCustomizer}
 * with the Spring Security filters ENABLED ({@code addFilters = true}) and security turned ON
 * ({@code spring.security.enabled=true}), which the Stage-0 {@link ApplicationContextSmokeTest}
 * deliberately does not do (it runs {@code addFilters = false} with security disabled, so it proves
 * nothing about live security behavior).
 *
 * <p><b>Why this whole class is env-gated.</b> When {@code spring.security.enabled=true}, the
 * {@code apiKeySetter()} factory bean in {@link Application} <i>requires</i> {@code ADMIN_API_KEY}
 * and {@code CONSUMER_API_KEY} to be present in the JVM environment, or context startup fails with
 * a {@code BeanCreationException}. Those keys are read from {@code System.getenv()} at bean-init
 * time, which Spring {@code properties}/{@code @DynamicPropertySource} cannot override. So this test
 * can only load its context where the two env vars are actually set. The {@code @BeforeClass} guard
 * {@code Assume}s their presence and skips the entire class cleanly otherwise (e.g. a plain local
 * {@code mvn test}). To run it deterministically — locally or in CI — export {@code ADMIN_API_KEY}
 * and {@code CONSUMER_API_KEY} before the build (see plan docs/634-modernization/634-stage3-boot27-plan.md
 * §3e / open question 2). The Stage-0 smoke test remains the always-on context-load gate.
 *
 * <p>Mirrors the smoke test's {@code @MockitoBean} set so the context loads without live AWS. The
 * {@code jwtDecoder()} bean stays on its fail-fast branch ({@code aws.cognito.user-pool-id=NONE});
 * no Bearer token is sent here, so that is fine. Assertions about the positive (valid-key / permitAll)
 * paths check that security did <i>not</i> reject the request (not 401/403); a downstream 200/400/500
 * from the mocked services is irrelevant to what the security chain is being asserted to do.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "spring.security.enabled=true")
@AutoConfigureMockMvc(addFilters = true)
@ActiveProfiles("test")
public class SecurityFilterChainIntegrationTest {

	@MockitoBean private DynamoDbClient dynamoDbClient;
	@MockitoBean private S3Client s3Client;
	@MockitoBean private CognitoAuthService cognitoAuthService;
	@MockitoBean private CognitoClientRegistry cognitoClientRegistry;
	@MockitoBean private ScienceMetrixService scienceMetrixService;
	@MockitoBean private ScienceMetrixDepartmentCategoryService scienceMetrixDepartmentCategoryService;
	@MockitoBean private DynamoDbMeshTermService dynamoDbMeshTermService;
	@MockitoBean private GenderService genderService;
	@MockitoBean private NameFrequencyService nameFrequencyService;
	@MockitoBean private DynamoDbInstitutionAfidService dynamoDbInstitutionAfidService;

    @Autowired private MockMvc mockMvc;

    /**
     * Gate the whole class: with security enabled, the context will not load unless both api keys
     * are in the environment (apiKeySetter requirement). Skip cleanly when they are absent so a
     * plain `mvn test` stays green.
     */
    @BeforeClass
    public static void requireApiKeysInEnv() {
        Assume.assumeNotNull(System.getenv("ADMIN_API_KEY"), System.getenv("CONSUMER_API_KEY"));
    }

    /** WebSecurityCustomizer always ignores /reciter/ping -> reachable with no api-key. */
    @Test
    public void pingIsPublic() throws Exception {
        mockMvc.perform(get("/reciter/ping")).andExpect(status().isOk());
    }

    /**
     * A protected /reciter/** path with no api-key must be REJECTED (MultiApiKeyFilter is in the
     * chain but sets no authentication when the header is absent; .anyRequest().authenticated()
     * then triggers the entry point). Expect 401 via CustomAuthenticationEntryPoint, tolerating
     * 403 in case the entry-point bean is ever not wired (plan §3e caveat 5).
     */
    @Test
    public void protectedPathRequiresKey() throws Exception {
        int sc = mockMvc.perform(get("/reciter/find/all/identity"))
                        .andReturn().getResponse().getStatus();
        org.junit.Assert.assertTrue("expected 401 or 403, got " + sc, sc == 401 || sc == 403);
    }

    /**
     * generate-access-token is reachable without an api-key (permitAll() in the chain AND
     * MultiApiKeyFilter.shouldNotFilter exempts it). Send a valid JSON content-type + minimal body
     * so the request reaches the @PostMapping (which requires consumes=APPLICATION_JSON); otherwise
     * a bare POST returns 415 and proves nothing. Assertion: security did NOT reject it (not 401/403).
     */
    @Test
    public void tokenEndpointIsReachableWithoutKey() throws Exception {
        int sc = mockMvc.perform(post("/reciter/generate-access-token")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                        .andReturn().getResponse().getStatus();
        org.junit.Assert.assertNotEquals("security must not reject this permitAll path", 401, sc);
        org.junit.Assert.assertNotEquals("security must not reject this permitAll path", 403, sc);
    }

    /**
     * A valid api-key PASSES the MultiApiKeyFilter (authenticated) so the request is not rejected by
     * security. We assert "not 401/403" rather than strictly 200, because the downstream controller
     * runs against mocked AWS services and may return 200/400/500 — none of which is a security
     * decision. The key is read from System.getenv (the @BeforeClass guarantees it is present).
     */
    @Test
    public void validKeyPassesSecurity() throws Exception {
        String adminKey = System.getenv("ADMIN_API_KEY");
        int sc = mockMvc.perform(get("/reciter/find/all/identity").header("api-key", adminKey))
                        .andReturn().getResponse().getStatus();
        org.junit.Assert.assertNotEquals("valid api-key must not be rejected by security", 401, sc);
        org.junit.Assert.assertNotEquals("valid api-key must not be rejected by security", 403, sc);
    }
}
