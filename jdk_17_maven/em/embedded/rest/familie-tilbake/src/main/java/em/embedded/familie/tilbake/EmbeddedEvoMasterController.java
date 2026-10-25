package em.embedded.familie.tilbake;

import no.nav.familie.tilbake.Launcher;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.OAuth2Config;
import no.nav.security.mock.oauth2.token.RequestMapping;
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String POSTGRES_VERSION = "13.13";

    private static final String POSTGRES_PASSWORD = "password";

    private static final int POSTGRES_PORT = 5432;

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer postgresContainer = new GenericContainer("postgres:" + POSTGRES_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust") //to allow all connections without a password
            .withEnv("POSTGRES_DB", "familietilbake")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
            .withExposedPorts(POSTGRES_PORT);

    private ConfigurableApplicationContext ctx;

    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

    private MockOAuth2Server oAuth2Server;
    private final String ISSUER_ID = "azuread";


    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    public static void main(String[] args) {
        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    @Override
    public boolean isSutRunning() {
        return ctx!=null && ctx.isRunning();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "no.nav.familie.tilbake.";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        //TODO
        return null;
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

    private OAuth2Config getOAuth2Config(){

        List<RequestMapping> mappings = Arrays.asList(
        );

        RequestMappingTokenCallback callback = new RequestMappingTokenCallback(
                ISSUER_ID,
                mappings,
                360000
        );

        Set<RequestMappingTokenCallback> callbacks = Set.of(
                callback
        );

        OAuth2Config config = new OAuth2Config(
                true,
                null,
                null,
                false,
                new no.nav.security.mock.oauth2.token.OAuth2TokenProvider(),
                callbacks
        );

        return config;
    }


    @Override
    public String startSut() {
        postgresContainer.start();

        String postgresURL = "jdbc:postgresql://" + postgresContainer.getHost() + ":" + postgresContainer.getMappedPort(POSTGRES_PORT) + "/familietilbake";

        oAuth2Server = new  MockOAuth2Server(getOAuth2Config());
        oAuth2Server.start(8081); //ephemeral gives issues in generated tests
        String wellKnownUrl = oAuth2Server.wellKnownUrl(ISSUER_ID).toString();

        System.setProperty("AZURE_APP_WELL_KNOWN_URL", wellKnownUrl);
        System.setProperty("AZURE_OPENID_CONFIG_TOKEN_ENDPOINT", "http://foo:8080/");
        System.setProperty("UNLEASH_SERVER_API_URL", "http://bar:8080/");
        System.setProperty("UNLEASH_SERVER_API_TOKEN", "71c722758740d43341c295ffdc237bd3");
        System.setProperty("NAIS_APP_NAME", "familietilbake");
        System.setProperty("NAIS_CLUSTER_NAME", "dev-gcp");
        System.setProperty("KAFKA_TRUSTSTORE_PATH", "dev-gcp");

        System.setProperty("AZURE_APP_CLIENT_ID","AZURE_APP_CLIENT_ID");
        System.setProperty("AZURE_APP_CLIENT_SECRET","AZURE_APP_CLIENT_SECRET");
        System.setProperty("FAMILIE_INTEGRASJONER_URL","http://FAMILIE_INTEGRASJONER_URL");
        System.setProperty("FAMILIE_INTEGRASJONER_SCOPE","FAMILIE_INTEGRASJONER_SCOPE");
        System.setProperty("PDL_URL","http://PDL_URL");
        System.setProperty("PDL_SCOPE","PDL_SCOPE");
        System.setProperty("FAMILIE_OPPDRAG_URL","http://FAMILIE_OPPDRAG_URL");
        System.setProperty("FAMILIE_OPPDRAG_SCOPE","FAMILIE_OPPDRAG_SCOPE");


        ctx = SpringApplication.run(Launcher.class, new String[]{
                "--logging.level.org.springframework.boot.context.properties.bind=DEBUG",
                "--server.port=0",
                "--spring.profiles.active=dev",
                "--management.server.port=-1",
                "--server.ssl.enabled=false",
                "--spring.datasource.url=" + postgresURL,
                "--spring.datasource.username=postgres",
                "--spring.datasource.password=" + POSTGRES_PASSWORD,
                "--sentry.logging.enabled=false",
                "--sentry.environment=local",
                "--logging.level.root=OFF",
                "--logging.config=classpath:logback-spring.xml",
                "--logging.level.org.springframework=INFO"
        });

        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            sqlConnection = DriverManager.getConnection(postgresURL, "postgres", POSTGRES_PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection));

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return (Integer) ((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public void stopSut() {
        postgresContainer.stop();
        if(ctx!=null) ctx.stop();
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }
}
