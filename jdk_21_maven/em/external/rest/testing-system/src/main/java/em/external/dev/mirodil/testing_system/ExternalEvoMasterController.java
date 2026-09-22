package em.external.dev.mirodil.testing_system;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String API_DOCS_PATH = "/api/v3/api-docs";

    private static final String SQL_DIR = "cs/rest/testing-system/docker/sql";

    private static final String POSTGRES_IMAGE = "postgres:17.2";

    private static final int POSTGRES_PORT = 5432;

    private static final String POSTGRES_DB = "testing_system";

    private static final String POSTGRES_USER = "root";

    private static final String POSTGRES_PASSWORD = "root";

    private static final String REDIS_IMAGE = "redis:7.4";

    private static final int REDIS_PORT = 6379;

    private static final String SECRET_KEY = "wfd-testing-system-jwt-signing-key-0123456789";

    private static final String WFD_ACCOUNTS =
            "INSERT INTO users (email, password, role_id, fname, lname, gender, status, created_at) VALUES "
                    + "('wfd_admin1@wfd.invalid', '$2a$10$lSC53c1jpEMh..ThMHNsOO4LKxBFgFWQ3KEoakTM1EyV5WHvOkDXG', 1, 'Wfd', 'Admin1', 'MALE', 'ACTIVE', '2025-01-20 10:00:00'), "
                    + "('wfd_admin2@wfd.invalid', '$2a$10$EW6Q/7e.ORkD2AfPEc0G4uW16dUkQiDm3ygb2fvqBIYrzk1B93mAq', 1, 'Wfd', 'Admin2', 'FEMALE', 'ACTIVE', '2025-01-20 10:01:00'), "
                    + "('wfd_user1@wfd.invalid', '$2a$10$.jEmFkrXZaBcMtaxdbw2jeXj0yQUguhZrycEHEm45tlX.kcb2gPpe', 2, 'Wfd', 'User1', 'MALE', 'ACTIVE', '2025-01-20 10:02:00'), "
                    + "('wfd_user2@wfd.invalid', '$2a$10$RlXMPGo2HheEsX5oWkrCYOJMlBXlbBAfyqaBq0Wa63qcmafJPoVE6', 2, 'Wfd', 'User2', 'FEMALE', 'ACTIVE', '2025-01-20 10:03:00');";

    private static final GenericContainer postgres = new GenericContainer(POSTGRES_IMAGE)
            .withEnv("POSTGRES_DB", POSTGRES_DB)
            .withEnv("POSTGRES_USER", POSTGRES_USER)
            .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath(SQL_DIR + "/01_create_tables.sql"),
                    "/docker-entrypoint-initdb.d/01_create_tables.sql")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
            .withExposedPorts(POSTGRES_PORT)
            .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2));

    private static final GenericContainer redis = new GenericContainer(REDIS_IMAGE)
            .withExposedPorts(REDIS_PORT);

    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/testing-system/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/testing-system-sut.jar";
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
        this(DEFAULT_CONTROLLER_PORT, "../target/testing-system-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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

    private String jdbcUrl() {
        return "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(POSTGRES_PORT)
                + "/" + POSTGRES_DB;
    }

    // Reuses the SUT's own seed file, so driver and SUT cannot drift apart.
    // EvoMaster splits the script on ";" alone and its parser rejects blank lines, hence the filter.
    private static String initSql() {
        try {
            return Files.readAllLines(Paths.get(SQL_DIR, "02_insert_data.sql"), StandardCharsets.UTF_8)
                    .stream()
                    .filter(line -> !line.trim().isEmpty())
                    .collect(Collectors.joining("\n")) + "\n" + WFD_ACCOUNTS;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--spring.datasource.url=" + jdbcUrl(),
                "--spring.datasource.username=" + POSTGRES_USER,
                "--spring.datasource.password=" + POSTGRES_PASSWORD,
                "--spring.data.redis.host=" + redis.getHost(),
                "--spring.data.redis.port=" + redis.getMappedPort(REDIS_PORT),
                "--logging.level.org.springframework.web=INFO",
                "--logging.level.org.springframework.jdbc.core.JdbcTemplate=INFO"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{"-DSECRET_KEY=" + SECRET_KEY};
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
        return "Started TestingSystemApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        postgres.start();
        redis.start();
    }

    @Override
    public void postStart() {
        closeDatabaseConnection();
        try {
            sqlConnection = DriverManager.getConnection(jdbcUrl(), POSTGRES_USER, POSTGRES_PASSWORD);
            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection)
                    .withInitSqlScript(initSql()));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preStop() {
        closeDatabaseConnection();
    }

    @Override
    public void postStop() {
        redis.stop();
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
        dbSpecification = null;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "dev.mirodil.testing_system.";
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
                login("WfdAdmin1", "wfd_admin1@wfd.invalid", "Wfd-Admin-Pass1"),
                login("WfdAdmin2", "wfd_admin2@wfd.invalid", "Wfd-Admin-Pass2"),
                login("WfdUser1", "wfd_user1@wfd.invalid", "Wfd-User-Pass1"),
                login("WfdUser2", "wfd_user2@wfd.invalid", "Wfd-User-Pass2")
        );
    }

    private static AuthenticationDto login(String name, String email, String password) {
        return AuthUtils.getForJsonTokenBearer(
                name,
                "/api/auth/login",
                "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}",
                "/content/token/access"
        );
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + API_DOCS_PATH,
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }
}
