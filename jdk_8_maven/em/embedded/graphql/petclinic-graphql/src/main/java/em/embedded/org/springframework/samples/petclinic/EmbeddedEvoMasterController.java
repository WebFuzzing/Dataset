package em.embedded.org.springframework.samples.petclinic;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.SqlScriptRunnerCached;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.GraphQlProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.samples.petclinic.PetClinicApplication;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class EmbeddedEvoMasterController extends EmbeddedSutController {

    public static void main(String[] args){

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        SutController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private ConfigurableApplicationContext ctx;
    private Connection sqlConnection;
    private String INIT_DB_SCRIPT_PATH = "/db/postgresql/populateDB.sql";

    private List<DbSpecification> dbSpecification;

    private String initSQLScript;

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer postgres = new GenericContainer("postgres:9")
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(5432)
            .withEnv("POSTGRES_HOST_AUTH_METHOD","trust")
            .withEnv("POSTGRES_DB", "petclinic")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));


    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        postgres.start();

        String host = postgres.getContainerIpAddress();
        int port = postgres.getMappedPort(5432);
        String url = "jdbc:postgresql://"+host+":"+port+"/petclinic";

        ctx = SpringApplication.run(PetClinicApplication.class, new String[]{
                "--server.port=0",
                "--spring.datasource.url=" + url,
                "--spring.cache.type=none",
                "--spring.profiles.active=postgresql,spring-data-jpa",
                "--spring.jmx.enabled=false",
        });

          if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
        try {
            sqlConnection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        // create tables
        SqlScriptRunnerCached.runScriptFromResourceFile(sqlConnection,"/db/postgresql/initDB.sql");

        /*
            ensure the data is empty
         */
        DbCleaner.clearDatabase_Postgres(sqlConnection,"public", null, null);

        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES,sqlConnection)
                .withSchemas("public")
                /*
                     evomaster not support to parse `on conflict` for postgresql yet
                     then cannot execute the sql script to initialize data in db

                     see https://www.postgresql.org/docs/current/sql-insert.html#:~:text=ON%20CONFLICT%20DO%20NOTHING%20simply,insertion%20as%20its%20alternative%20action.
                 */
                .withInitSqlOnResourcePath(INIT_DB_SCRIPT_PATH)
        );



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
        postgres.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.springframework.samples.petclinic";
    }

    @Override
    public void resetStateOfSUT() {
//        DbCleaner.clearDatabase_Postgres(sqlConnection,"public", null);
//        SqlScriptRunnerCached.runScriptFromResourceFile(sqlConnection,"/db/postgresql/populateDB.sql");
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new GraphQlProblem("/graphql");
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }


    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }
}
