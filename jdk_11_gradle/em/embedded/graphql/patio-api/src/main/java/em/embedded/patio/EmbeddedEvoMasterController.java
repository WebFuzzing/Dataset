package em.embedded.patio;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.GraphQlProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;
import patio.Application;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

    private ApplicationContext ctx;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

    private final String INIT_DB_SCRIPT_PATH = "/initDB.sql";


    private final int portApp = 8080; //Hardcoded. will need fixing
    // TODO maybe report at https://github.com/micronaut-projects/micronaut-core/issues


    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer postgres = new GenericContainer("postgres:9")
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(5432)
            .withEnv("POSTGRES_HOST_AUTH_METHOD","trust")
            .withEnv("POSTGRES_DB", "patio")
            .withEnv("POSTGRES_USER", "patio")
            .withEnv("POSTGRES_PASSWORD", "patio")
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
        String url = "jdbc:postgresql://"+host+":"+port+"/patio";

        ctx = Micronaut.run(Application.class, new String[]{
                "-micronaut.server.port="+portApp,
                "-datasources.default.url=" + url
        });


        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            sqlConnection = DriverManager.getConnection(url, "patio", "patio");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        /*
                ensure the data is empty
             */
        DbCleaner.clearDatabase_Postgres(sqlConnection, "public", List.of("flyway_schema_history"), null);

        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES,sqlConnection)
                .withSchemas("public").withInitSqlOnResourcePath(INIT_DB_SCRIPT_PATH));

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return portApp;
        //FIXME this will need to be fixed
//        return (Integer) ((Map) ctx.getEnvironment()
//                .getPropertySources().stream().findFirst().get())
//                .get("local.server.port");
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
        return "patio.";
    }

    @Override
    public void resetStateOfSUT() {
//        DbCleaner.clearDatabase_Postgres(sqlConnection, "public", List.of("flyway_schema_history"));
//        SqlScriptRunnerCached.runScriptFromResourceFile(sqlConnection,"/initDB.sql");
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new GraphQlProblem("/graphql");
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {

        return List.of(AuthUtils.getForJWT(
                "Stark",
                "/graphql",
                "{\"query\": \"{login(email: \\\"tstark@email.com\\\",password: \\\"avengers\\\"){tokens{authenticationToken}}}\"}",
                "/data/login/tokens/authenticationToken"
        ));
    }





    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

}
