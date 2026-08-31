package em.external.com.mongodb.starter;


import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final int DEFAULT_DB_PORT = 27017;

    private static final String MONGODB_VERSION = "7.0";

    //private static final String MONGODB_DATABASE_NAME = "HospitalDB";

    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/person-controller/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/person-controller-sut.jar";
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

    private MongoClient mongoClient;

    private final GenericContainer<?> mongodb;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/person-controller-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
        this.mongodb = new GenericContainer<>("mongo:" + MONGODB_VERSION)
                .withTmpFs(Collections.singletonMap("/data/db", "rw"))
                .withExposedPorts(DEFAULT_DB_PORT);
        setJavaCommand(command);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--spring.data.mongodb.uri=mongodb://" + mongodb.getContainerIpAddress() + ":" + mongodb.getMappedPort(DEFAULT_DB_PORT), // + "/" + MONGODB_DATABASE_NAME,
                "--spring.cache.type=NONE"
        };
    }


    private String dbUrl() {
        return null;
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{
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
        return "Started ApplicationStarter in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {

        mongodb.start();

        try {
            mongoClient = MongoClients.create("mongodb://" + mongodb.getContainerIpAddress() + ":" + mongodb.getMappedPort(DEFAULT_DB_PORT));
        } catch (Exception e) {
            System.out.println("ERROR: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    @Override
    public void postStart() {

    }

    @Override
    public void preStop() {

    }

    @Override
    public void postStop() {
        mongodb.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.mongodb.starter.";
    }


    public void resetStateOfSUT() {
       // mongoClient.getDatabase(MONGODB_DATABASE_NAME).drop();
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/v3/api-docs",
                null
        );
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

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }


}
