package em.embedded.dev.mirodil.testing_system;

import dev.mirodil.testing_system.TestingSystemApplication;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

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
        redis.start();

        System.setProperty("SECRET_KEY", SECRET_KEY);

        ctx = SpringApplication.run(TestingSystemApplication.class, new String[]{
                "--server.port=0",
                "--spring.datasource.url=" + jdbcUrl(),
                "--spring.datasource.username=" + POSTGRES_USER,
                "--spring.datasource.password=" + POSTGRES_PASSWORD,
                "--spring.data.redis.host=" + redis.getHost(),
                "--spring.data.redis.port=" + redis.getMappedPort(REDIS_PORT),
                "--spring.devtools.restart.enabled=false",
                "--logging.level.org.springframework.web=INFO",
                "--logging.level.org.springframework.jdbc.core.JdbcTemplate=INFO"
        });

        closeDatabaseConnection();
        try {
            sqlConnection = DriverManager.getConnection(jdbcUrl(), POSTGRES_USER, POSTGRES_PASSWORD);
            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES, sqlConnection)
                    .withInitSqlScript(initSql()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
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

    // The JWT is wrapped twice: {"content": {"user": ..., "token": {"access": ...}}}
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
                "http://localhost:" + getSutPort() + API_DOCS_PATH,
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }
}
