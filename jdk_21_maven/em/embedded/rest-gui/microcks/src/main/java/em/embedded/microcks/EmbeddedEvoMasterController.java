package em.embedded.microcks;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.microcks.MicrocksApplication;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
 */


/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    public static void main(String[] args) {

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private ConfigurableApplicationContext ctx;

    private static final int MONGODB_PORT = 27017;
    private static final int KEYCLOAK_PORT = 8080;
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String REALM_JSON_PATH = "/opt/keycloak/data/import/microcks-realm.json";
    private static final String POSTMAN_IMAGE = "quay.io/microcks/microcks-postman-runtime:0.6.0";
    private static final int POSTMAN_PORT = 3000;

    private static final String MONGODB_VERSION = "7.0";

    private static final String MONGODB_DATABASE_NAME = "test";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodbContainer = new GenericContainer("mongo:" + MONGODB_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withExposedPorts(MONGODB_PORT);

    private static final GenericContainer keycloakContainer = new GenericContainer(
            DockerImageName.parse("quay.io/keycloak/keycloak:26.0.0")
    )
            .withExposedPorts(KEYCLOAK_PORT)
            .withEnv("KEYCLOAK_ADMIN", ADMIN_USER)
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", ADMIN_PASSWORD)
            .withEnv("KC_HEALTH_ENABLED", "true")
            .withEnv("KC_METRICS_ENABLED", "true")
            .withCommand(
                    "start-dev",
                    "--hostname-strict=false",
                    "--import-realm",
                    "--health-enabled=true"
            )
            .withClasspathResourceMapping("microcks-realm-sample.json", REALM_JSON_PATH, BindMode.READ_ONLY)
            .waitingFor(Wait.forListeningPort());

    private static final GenericContainer<?> postmanContainer = new GenericContainer<>(DockerImageName.parse(POSTMAN_IMAGE))
            .withExposedPorts(POSTMAN_PORT)
            .waitingFor(Wait.forHttp("/health")
                    .forPort(POSTMAN_PORT)
                    .withStartupTimeout(Duration.ofSeconds(30)))
            .withStartupTimeout(Duration.ofSeconds(30));

    private MongoClient mongoClient;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        mongodbContainer.start();
        keycloakContainer.start();
        postmanContainer.start();

        mongoClient = MongoClients.create("mongodb://" + mongodbContainer.getContainerIpAddress() + ":" + mongodbContainer.getMappedPort(MONGODB_PORT));

        System.setProperty("POSTMAN_RUNNER_URL", "http://" + postmanContainer.getContainerIpAddress() + ":" + postmanContainer.getMappedPort(POSTMAN_PORT));
        System.setProperty("SERVICES_UPDATE_INTERVAL", "0 0 0/2 * * *");
        System.setProperty("KEYCLOAK_URL", "http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT));
        System.setProperty("KEYCLOAK_PUBLIC_URL", "http://localhost:" + keycloakContainer.getMappedPort(KEYCLOAK_PORT));
        System.setProperty("ENABLE_CORS_POLICY", "false");
        System.setProperty("CORS_REST_ALLOW_CREDENTIALS", "true");

        ctx = SpringApplication.run(MicrocksApplication.class,
                new String[]{"--server.port=0",
                        "--spring.profiles.active=prod",
                        "--grpc.server.port=0",
                        "--spring.data.mongodb.uri=" + "mongodb://" +  mongodbContainer.getContainerIpAddress() + ":" + mongodbContainer.getMappedPort(MONGODB_PORT) + "/" + MONGODB_DATABASE_NAME,
                        "--spring.security.oauth2.resourceserver.jwt.issuer-uri=http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT) + "/realms/microcks",
                        "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT) + "/realms/microcks/protocol/openid-connect/certs"
                });

        System.setProperty("TEST_CALLBACK_URL", "http://localhost:" + getSutPort());

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }


    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public void stopSut() {
        ctx.stop();
        ctx.close();

        mongodbContainer.stop();
        keycloakContainer.stop();
        postmanContainer.stop();

    }

    @Override
    public String getPackagePrefixesToCover() {
        return "io.github.microcks.";
    }

    @Override
    public void resetStateOfSUT() {
        mongoClient.getDatabase(MONGODB_DATABASE_NAME).drop();
    }


    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        //http://localhost:{port}/realms/microcks/protocol/openid-connect/token
        String postEndpoint = "http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT) + "/realms/microcks/protocol/openid-connect/token";

        String payloadTemplate = "username=%s&password=microcks123&grant_type=password&client_id=microcks-serviceaccount&client_secret=ab54d329-e435-41ae-a900-ec6b3fe15c54";

        return Arrays.asList(
                AuthUtils.getForJsonToken("ADMIN",
                        postEndpoint,
                        String.format(payloadTemplate, "admin"),
                        "/access_token",
                        "Bearer ",
                        "application/x-www-form-urlencoded"),
                AuthUtils.getForJsonToken("ADMIN_2",
                        postEndpoint,
                        String.format(payloadTemplate, "admin2"),
                        "/access_token",
                        "Bearer ",
                        "application/x-www-form-urlencoded")
        );
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }
}
