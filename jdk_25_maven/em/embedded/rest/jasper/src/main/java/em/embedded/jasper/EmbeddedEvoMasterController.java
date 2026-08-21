package em.embedded.jasper;

import com.webfuzzing.commons.auth.Header;
import jasper.JasperApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String API_DOCS_PATH = "/api/v3/api-docs";

    private static final String POSTGRES_VERSION = "18";

    private static final int POSTGRES_PORT = 5432;

    private static final String POSTGRES_DB = "jasper";

    private static final String POSTGRES_USER = "postgres";

    private static final String POSTGRES_PASSWORD = "password";


    private static final GenericContainer postgres = new GenericContainer("postgres:" + POSTGRES_VERSION)
            .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
            .withEnv("POSTGRES_DB", POSTGRES_DB)
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql", "rw"))
            .withExposedPorts(POSTGRES_PORT);


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

    private Connection sqlConnection;

    private List<DbSpecification> dbSpecification;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        postgres.start();

        ctx = SpringApplication.run(JasperApplication.class, new String[]{
                "--server.port=0",
                "--spring.profiles.active=api-docs",
                "--spring.datasource.url=" + dbUrl(),
                "--spring.datasource.username=" + POSTGRES_USER,
                "--spring.datasource.password=" + POSTGRES_PASSWORD,
                "--spring.datasource.hikari.auto-commit=false",
                "--spring.jpa.database-platform=jasper.config.PostgreSQLDialect",
                "--springdoc.api-docs.path=" + API_DOCS_PATH,
                "--jasper.allow-user-tag-header=true",
                "--jasper.allow-user-role-header=true",
                "--jasper.allow-auth-headers=true"
        });

        closeDatabaseConnection();
        try {
            sqlConnection = DriverManager.getConnection(dbUrl(), POSTGRES_USER, POSTGRES_PASSWORD);
            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
    }

    private String dbUrl() {
        String host = postgres.getHost();
        int port = postgres.getMappedPort(POSTGRES_PORT);
        return "jdbc:postgresql://" + host + ":" + port + "/" + POSTGRES_DB;
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
        closeDatabaseConnection();

        if (ctx != null) {
            ctx.stop();
            ctx.close();
            ctx = null;
        }

        postgres.stop();
    }

    private void closeDatabaseConnection() {
        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sqlConnection = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "jasper.";
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                auth("admin", "+user/admin", "ROLE_ADMIN"),
                auth("alice", "+user/alice", "ROLE_USER"),
                auth("bob", "+user/bob", "ROLE_USER")
        );
    }

    /*
        CSRF uses a cookie-based repository, so a request is accepted as long as the header
        and the cookie carry the same value. Without them every write answers 403.
     */
    private static final String CSRF_TOKEN = "evomaster";

    private static AuthenticationDto auth(String name, String userTag, String role) {
        AuthenticationDto dto = new AuthenticationDto(name);
        dto.getFixedHeaders().add(header("User-Tag", userTag));
        dto.getFixedHeaders().add(header("User-Role", role));
        dto.getFixedHeaders().add(header("X-XSRF-TOKEN", CSRF_TOKEN));
        dto.getFixedHeaders().add(header("Cookie", "XSRF-TOKEN=" + CSRF_TOKEN));
        return dto;
    }

    private static Header header(String name, String value) {
        Header h = new Header();
        h.setName(name);
        h.setValue(value);
        return h;
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + API_DOCS_PATH + "/jasper",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }
}
