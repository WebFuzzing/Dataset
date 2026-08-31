package em.external.microcks;


import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String MONGODB_VERSION = "7.0";

    private static final int KEYCLOAK_PORT = 8080;
    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String REALM_JSON_PATH = "/opt/keycloak/data/import/microcks-realm.json";
    private static final String POSTMAN_IMAGE = "quay.io/microcks/microcks-postman-runtime:0.6.0";
    private static final int POSTMAN_PORT = 3000;
    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE_NAME = "test";


    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest-gui/microcks/webapp/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/microcks-sut.jar";
        }

        int timeoutSeconds = 120;
        if (args.length > 3) {
            timeoutSeconds = Integer.parseInt(args[3]);
        }

        String command = "java";
        if (args.length > 4) {
            command = args[4];
        }

        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        controller.setNeedsJdk17Options(true);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int timeoutSeconds;

    private final int sutPort;


    private String jarLocation;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

    private MongoClient mongoClient;

    private final GenericContainer<?> mongodb;
    private final GenericContainer<?> keycloakContainer;
    private final GenericContainer<?> postmanContainer;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "cs/rest-gui/microcks/webapp/target/microcks-sut.jar", DEFAULT_SUT_PORT, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command) {
        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);

        this.mongodb = new GenericContainer<>("mongo:" + MONGODB_VERSION)
                .withTmpFs(Collections.singletonMap("/data/db", "rw"))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig()
                                .withPortBindings(new PortBinding(
                                        Ports.Binding.bindPort(sutPort + 3),
                                        new ExposedPort(MONGODB_PORT)
                                ))
                ))
                .withExposedPorts(MONGODB_PORT);

        this.keycloakContainer = new GenericContainer<>(
                DockerImageName.parse("quay.io/keycloak/keycloak:26.0.0")
        )
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig()
                                .withPortBindings(new PortBinding(
                                        Ports.Binding.bindPort(sutPort + 2),
                                        new ExposedPort(KEYCLOAK_PORT)
                                ))
                ))
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

        this.postmanContainer = new GenericContainer<>(DockerImageName.parse(POSTMAN_IMAGE))
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig()
                                .withPortBindings(new PortBinding(
                                        Ports.Binding.bindPort(sutPort + 1),
                                        new ExposedPort(POSTMAN_PORT)
                                ))
                ))
                .withExposedPorts(POSTMAN_PORT)
                .waitingFor(Wait.forHttp("/health")
                        .forPort(POSTMAN_PORT)
                        .withStartupTimeout(Duration.ofSeconds(30)))
                .withStartupTimeout(Duration.ofSeconds(30));
        this.setNeedsJdk17Options(true);
        setJavaCommand(command);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{"--server.port="+sutPort,
                "--spring.profiles.active=prod",
                "--grpc.server.port=0",
                "--spring.data.mongodb.uri=" + "mongodb://" +  mongodb.getContainerIpAddress() + ":" + mongodb.getMappedPort(MONGODB_PORT) + "/" + MONGODB_DATABASE_NAME,
                "--spring.security.oauth2.resourceserver.jwt.issuer-uri=http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT) + "/realms/microcks",
                "--spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT) + "/realms/microcks/protocol/openid-connect/certs"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{
                "-DPOSTMAN_RUNNER_URL="+"http://" + postmanContainer.getContainerIpAddress() + ":" + postmanContainer.getMappedPort(POSTMAN_PORT),
                "-DSERVICES_UPDATE_INTERVAL='0 0 0/2 * * *'",
                "-DKEYCLOAK_URL=" + "http://" + keycloakContainer.getContainerIpAddress() + ":" + keycloakContainer.getMappedPort(KEYCLOAK_PORT),
                "-DKEYCLOAK_PUBLIC_URL=" + "http://localhost:" + keycloakContainer.getMappedPort(KEYCLOAK_PORT),
                "-DENABLE_CORS_POLICY=false",
                "-DCORS_REST_ALLOW_CREDENTIALS=true",
                "-DTEST_CALLBACK_URL=" + "http://localhost:" + sutPort
        };
    }


    @Override
    public String getBaseURL() {
        return "http://localhost:" + sutPort;
    }

    @Override
    public String getPathToExecutableJar() {
        return jarLocation;
    }

    @Override
    public String getLogMessageOfInitializedServer() {
        return "Started MicrocksApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {

        mongodb.start();
        postmanContainer.start();
        keycloakContainer.start();

        try {
            mongoClient = MongoClients.create("mongodb://" + mongodb.getContainerIpAddress() + ":" + mongodb.getMappedPort(MONGODB_PORT));
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    public void postStart() {

    }

    @Override
    public void preStop() {

    }

    @Override
    public void postStop() {
        mongodb.stop();
        keycloakContainer.stop();
        postmanContainer.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "io.github.microcks.";
    }

    public void resetStateOfSUT() {
        mongoClient.getDatabase(MONGODB_DATABASE_NAME).drop();
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
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
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }


}
