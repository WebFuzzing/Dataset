package em.external.org.devgateway.ocvn;


import com.mongodb.MongoClient;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.SqlScriptRunnerCached;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.h2.tools.Server;
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
        String jarLocation = "cs/rest-gui/ocvn/web/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/ocvn-sut.jar";
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
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private final int timeoutSeconds;
    private final int sutPort;
    private final int dbPort;

    private  String jarLocation;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;
    private Server h2;

    private MongoClient mongoClient;

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodb = new GenericContainer("mongo:3.2")
            .withCreateContainerCmdModifier(modifier)
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withExposedPorts(27017);

    public ExternalEvoMasterController() {
        this(40100, "../web/target/web-1.1.1-SNAPSHOT-exec.jar", 12345, 120, "java");
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
        this.dbPort = sutPort + 2;
        setJavaCommand(command);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{"--server.port=" + sutPort};
    }


    private String dbUrl( ) {

        String url = "jdbc";
        url += ":h2:tcp://localhost:" + dbPort + "/mem:testdb_" + dbPort;

        return url;
    }

    @Override
    public String[] getJVMParameters() {


        return new String[]{
                "-Dliquibase.enabled=false",
                "-Dspring.data.mongodb.uri=mongodb://"+mongodb.getContainerIpAddress()+":"+mongodb.getMappedPort(27017)+"/ocvn",
                "-Dspring.datasource.url=" + dbUrl() + ";DB_CLOSE_DELAY=-1",
                "-Dspring.datasource.driver-class-name=org.h2.Driver",
                "-Dspring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "-Dspring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
                "-Dspring.datasource.username=sa",
                "-Dspring.datasource.password",
                "-Ddg-toolkit.derby.port=0",
                "-Dspring.cache.type=NONE",
                "-Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"
        };
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
        return "Started WebApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {

        mongodb.start();

        try {
            mongoClient = new MongoClient(mongodb.getContainerIpAddress(),
                    mongodb.getMappedPort(27017));

        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }

        try {
            //starting H2
            h2 = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "" + dbPort);
            h2.start();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
    public void postStart() {
        closeDataBaseConnection();

        try {
            Class.forName("org.h2.Driver");
            sqlConnection = DriverManager.getConnection(dbUrl(), "sa", "");
            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.H2,sqlConnection)
                    .withInitSqlOnResourcePath("/init_db.sql"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preStop() {
        closeDataBaseConnection();
    }

    @Override
    public void postStop() {
        mongodb.stop();
        if (h2 != null) {
            h2.stop();
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.devgateway.";
    }


    public void resetStateOfSUT() {
        mongoClient.getDatabase("ocvn").drop();
        mongoClient.getDatabase("ocvn-shadow").drop();

//        DbCleaner.clearDatabase_H2(connection);
//        SqlScriptRunnerCached.runScriptFromResourceFile(connection,"/init_db.sql");
    }



    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/v2/api-docs?group=1ocDashboardsApi",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {

        return Arrays.asList(
                AuthUtils.getForDefaultSpringFormLogin("ADMIN", "admin", "admin"),
                AuthUtils.getForDefaultSpringFormLogin("user1", "user1", "password"));
    }



    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

    @Override
    public Object getMongoConnection() {return mongoClient;}
}
