package em.external.com.example.demo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "flightdatabase";

    // The DB starts empty (the flight loader is never scheduled); airports and flights can be created via the API
    private static final List<Document> USERS = List.of(
            user("wfd_admin1", "$2a$10$2xOaY0XmjU8VrHnQ6ORmme.4pAD21fsnoM9dcf8Y8NYwmXZztsDsC", "ADMIN"),
            user("wfd_admin2", "$2a$10$qKrL3SboXVhfaaevAMiU.eFAj1wD4.X2VVvI.XSmjVlTPf8.95Z9y", "ADMIN"),
            user("wfd_user1", "$2a$10$t.OcuL3NBcdCtRCUtX4SiuG6RPgGxs0yQpdvVl11J0Mv249/vu9Za", "USER"),
            user("wfd_user2", "$2a$10$1WlQAEEMP97IO1o8vLl3ROrLeF9hdKRjd0yxlOve.ZhF.HCW9rES.", "USER")
    );

    private static Document user(String id, String passwordHash, String type) {
        return new Document("_id", id)
                .append("EMAIL", id + "@wfd.invalid")
                .append("PASSWORD", passwordHash)
                .append("FIRST_NAME", id)
                .append("LAST_NAME", id)
                .append("PHONE_NUMBER", "05550000000")
                .append("USER_TYPE", type)
                .append("USER_STATUS", "ACTIVE")
                .append("_class", "com.example.demo.auth.model.entity.UserEntity");
    }

    private static final GenericContainer mongodb = new GenericContainer(MONGODB_IMAGE)
            .withExposedPorts(MONGODB_PORT);

    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/flight-search-api/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/flight-search-api-sut.jar";
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

    private MongoClient mongoClient;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/flight-search-api-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
                "--spring.data.mongodb.host=" + mongodb.getHost(),
                "--spring.data.mongodb.port=" + mongodb.getMappedPort(MONGODB_PORT),
                "--spring.data.mongodb.database=" + MONGODB_DATABASE
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{};
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
        return "Started FlightSearchApiApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        mongodb.start();
        mongoClient = MongoClients.create("mongodb://" + mongodb.getHost() + ":" + mongodb.getMappedPort(MONGODB_PORT));
        seedUsers();
    }

    private void seedUsers() {
        List<Document> users = new ArrayList<>();
        for (Document u : USERS) {
            users.add(new Document(u));
        }
        mongoClient.getDatabase(MONGODB_DATABASE).getCollection("user-collection").insertMany(users);
    }

    @Override
    public void postStart() {
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        mongodb.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.demo.";
    }

    @Override
    public void resetStateOfSUT() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            db.getCollection(name).deleteMany(new Document());
        }
        seedUsers();
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    private static AuthenticationDto login(String name, String email, String password) {
        return AuthUtils.getForJsonTokenBearer(
                name,
                "/api/v1/authentication/user/login",
                "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}",
                "/response/accessToken"
        );
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of(
                login("WfdAdmin1", "wfd_admin1@wfd.invalid", "Wfd-Admin-Pass1"),
                login("WfdAdmin2", "wfd_admin2@wfd.invalid", "Wfd-Admin-Pass2"),
                login("WfdUser1", "wfd_user1@wfd.invalid", "Wfd-User-Pass1"),
                login("WfdUser2", "wfd_user2@wfd.invalid", "Wfd-User-Pass2")
        );
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
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
