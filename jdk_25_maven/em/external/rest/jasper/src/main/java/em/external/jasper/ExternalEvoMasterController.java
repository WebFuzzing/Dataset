package em.external.jasper;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    public static void main(String[] args) {
        int controllerPort = 40100;
        int sutPort = 8080;
        String jarLocation = "jdk_25_maven/cs/rest/jasper/target/jasper-sut.jar";
        int timeoutSeconds = 120;
        String command = "java";

        if (args.length > 0) controllerPort = Integer.parseInt(args[0]);
        if (args.length > 1) sutPort = Integer.parseInt(args[1]);
        if (args.length > 2) jarLocation = args[2];
        if (args.length > 3) timeoutSeconds = Integer.parseInt(args[3]);
        if (args.length > 4) command = args[4];

        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        controller.setNeedsJdk17Options(true);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    private final int sutPort;
    private final int timeoutSeconds;
    private String jarLocation;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecifications;

    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));

    public ExternalEvoMasterController() {
        this(40100, "jdk_25_maven/cs/rest/jasper/target/jasper-sut.jar", 8080, 120, "java");
    }

    public ExternalEvoMasterController(int controllerPort, String jarLocation, int sutPort,
                                       int timeoutSeconds, String command) {
        this.sutPort = sutPort;
        this.timeoutSeconds = timeoutSeconds;
        this.jarLocation = jarLocation;
        setControllerPort(controllerPort);
        setJavaCommand(command);
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
        return "Started JasperApplication";
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
        closeConnection();
        try {
            Class.forName("org.postgresql.Driver");
            sqlConnection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword());
            dbSpecifications = List.of(new DbSpecification(DatabaseType.POSTGRES, sqlConnection));
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preStop() {
        closeConnection();
    }

    @Override
    public void postStop() {
        postgres.stop();
    }

    private void closeConnection() {
        if (sqlConnection != null) {
            try { sqlConnection.close(); } catch (SQLException e) { e.printStackTrace(); }
            sqlConnection = null;
        }
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--spring.profiles.active=dev,api-docs,evomaster",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.jpa.database-platform=jasper.config.PostgreSQLDialect",
                "--jasper.default-role=ROLE_ADMIN",
                "--jasper.allow-auth-headers=true",
                "--jasper.allow-user-role-header=true",
                "--spring.mail.host=",
                "--spring.cache.type=NONE",
                "--springdoc.show-actuator=false",
                "--management.server.port=-1"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{};
    }

    @Override
    public void resetStateOfSUT() {
        try {
            sqlConnection.createStatement().execute(
                    "DO $$ DECLARE r RECORD; BEGIN " +
                    "FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public') LOOP " +
                    "EXECUTE 'TRUNCATE TABLE ' || quote_ident(r.tablename) || ' CASCADE'; " +
                    "END LOOP; END $$;");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecifications;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of();
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "jasper.";
    }
}
