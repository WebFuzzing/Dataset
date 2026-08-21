package em.external.jasper;


import com.webfuzzing.commons.auth.Header;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String POSTGRES_VERSION = "18";

    private static final String API_DOCS_PATH = "/api/v3/api-docs";

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

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/jasper/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/jasper-sut.jar";
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

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/jasper-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
        setJavaCommand(command);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
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
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{};
    }


    private String dbUrl() {
        String host = postgres.getHost();
        int port = postgres.getMappedPort(POSTGRES_PORT);
        return "jdbc:postgresql://" + host + ":" + port + "/" + POSTGRES_DB;
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
        return "Started JasperApplication in";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        postgres.start();
    }

    @Override
    public void postStart() {
        closeDatabaseConnection();

        try {
            sqlConnection = DriverManager.getConnection(dbUrl(), POSTGRES_USER, POSTGRES_PASSWORD);
            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preStop() {
        closeDatabaseConnection();
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
    public void postStop() {
        postgres.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "jasper.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + API_DOCS_PATH + "/jasper",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    /*
        Jasper has no user registration: an identity is whatever the User-Tag and User-Role
        headers say, once the SUT is started with the allow-*-header flags above.
        "alice" and "bob" share the same role on purpose, so that broken access control
        between two users of equal privilege is detectable.
     */
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
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

}
