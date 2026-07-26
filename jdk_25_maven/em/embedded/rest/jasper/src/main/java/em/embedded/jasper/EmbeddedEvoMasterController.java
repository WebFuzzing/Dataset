package em.embedded.jasper;

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
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;


/**
 * EvoMaster embedded driver for Jasper (https://github.com/cjmalloy/jasper).
 * Jasper is a knowledge graph REST API built with Spring Boot 4 + PostgreSQL.
 *
 * Auth: jasper.default-role=ROLE_ADMIN grants all requests admin access without JWT.
 * Redis: RedisConfig is @Profile("redis") — not activated here, so no Redis needed.
 * K8s:  spring.cloud.kubernetes.discovery.enabled=false is already the default in application.yml.
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
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecifications;

    private static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16")
                    .withTmpFs(Map.of("/var/lib/postgresql/data", "rw"));

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {
        postgres.start();

        try {
            sqlConnection = DriverManager.getConnection(
                    postgres.getJdbcUrl(),
                    postgres.getUsername(),
                    postgres.getPassword());
            dbSpecifications = List.of(new DbSpecification(
                    DatabaseType.POSTGRES,
                    sqlConnection));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        ctx = SpringApplication.run(JasperApplication.class,
                "--server.port=0",
                "--spring.profiles.active=dev,api-docs",
                "--spring.datasource.url=" + postgres.getJdbcUrl(),
                "--spring.datasource.username=" + postgres.getUsername(),
                "--spring.datasource.password=" + postgres.getPassword(),
                "--spring.jpa.database-platform=jasper.config.PostgreSQLDialect",
                // Grant all unauthenticated requests ROLE_ADMIN — no JWT needed
                "--jasper.default-role=ROLE_ADMIN",
                "--jasper.allow-auth-headers=true",
                "--jasper.allow-user-role-header=true",
                // Disable mail, cache and other non-essential features
                "--spring.mail.host=",
                "--spring.cache.type=NONE",
                "--management.server.port=-1"
        );

        return "http://localhost:" + getSutPort();
    }

    private int getSutPort() {
        return (Integer) ((Map<?, ?>) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public void stopSut() {
        if (ctx != null) {
            ctx.stop();
            ctx.close();
        }
        try {
            if (sqlConnection != null) sqlConnection.close();
        } catch (SQLException e) {
            // ignore on shutdown
        }
        postgres.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "jasper.";
    }

    @Override
    public void resetStateOfSUT() {
        // Liquibase manages the schema; truncate all data tables between EvoMaster runs
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
        // jasper.default-role=ROLE_ADMIN means all requests are already admin.
        // No token injection needed.
        return List.of();
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
}
