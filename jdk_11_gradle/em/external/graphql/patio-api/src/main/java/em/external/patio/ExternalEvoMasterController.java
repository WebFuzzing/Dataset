package em.external.patio;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;

import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.GraphQlProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {


    public static void main(String[] args) {

        int controllerPort = 40100;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = 12345;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/graphql/patio-api/build/libs";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/patio-api-sut.jar";
        }

        int timeoutSeconds = 120;
        if(args.length > 3){
            timeoutSeconds = Integer.parseInt(args[3]);
        }
        String command = "java";
        if(args.length > 4){
            command = args[4];
        }


        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation,
                        sutPort, timeoutSeconds, command);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private final int timeoutSeconds;
    private final int sutPort;
    private  String jarLocation;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

    private final String INIT_DB_SCRIPT_PATH = "/initDB.sql";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer postgres = new GenericContainer("postgres:9")
            .withCreateContainerCmdModifier(modifier)
            .withEnv("POSTGRES_HOST_AUTH_METHOD","trust")
            .withEnv("POSTGRES_DB", "patio")
            .withEnv("POSTGRES_USER", "patio")
            .withEnv("POSTGRES_PASSWORD", "patio")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"));

    public ExternalEvoMasterController(){
        this(40100, "../core/target", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(
            int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command
           ) {

        if(jarLocation==null || jarLocation.isEmpty()){
            throw new IllegalArgumentException("Missing jar location");
        }


        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
        setJavaCommand(command);

    }


    @Override
    public String[] getInputParameters() {
        return new String[]{
                "-micronaut.server.port="+sutPort,
                "-datasources.default.url=" + dbUrl()
        };
    }

    public String[] getJVMParameters() {
        return new String[]{};
    }

    private String dbUrl() {

        String host = postgres.getContainerIpAddress();
        int port = postgres.getMappedPort(5432);

        String url = "jdbc";
        url += ":postgresql://"+host+":"+port+"/patio";

        return url;
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
        return "Startup completed in";
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
        closeDataBaseConnection();

        try {
            sqlConnection = DriverManager.getConnection(dbUrl(), "patio", "patio");

            /*
                ensure the data is empty
             */
            DbCleaner.clearDatabase_Postgres(sqlConnection, "public", List.of("flyway_schema_history"), null);

            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES,sqlConnection)
                    .withSchemas("public").withInitSqlOnResourcePath(INIT_DB_SCRIPT_PATH));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resetStateOfSUT() {
//        DbCleaner.clearDatabase_Postgres(sqlConnection, "public", List.of("flyway_schema_history"));
//        SqlScriptRunnerCached.runScriptFromResourceFile(sqlConnection,"/initDB.sql");
    }

    @Override
    public void preStop() {
        closeDataBaseConnection();
    }

    @Override
    public void postStop() {
        postgres.stop();
    }

    private void closeDataBaseConnection() {
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
        return "patio.";
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

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }
}
