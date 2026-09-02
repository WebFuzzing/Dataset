package em.external.com.example.joinUs;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import com.webfuzzing.commons.auth.LoginEndpoint;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.AuthTokens;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "joinUs";

    private static final String NEO4J_IMAGE = "neo4j:5.26";

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final String NEO4J_USERNAME = "neo4j";

    private static final String NEO4J_PASSWORD = "wfdNeo4jPass123";

    // Login is by member id, and only a document written directly to Mongo can carry isAdmin=true
    // (/register always stores false). Same two accounts as the BB seed files.
    private static final List<Document> USERS = List.of(
            user("wfd_admin", "$2a$10$wMFYFpqtbELhPxBYXz/Cy.0qhbq/C1H0RRjVZKmSzdAulP8tCCtLS", true),
            user("wfd_user", "$2a$10$p5FTsGJHk1g1pCncGxcgFeTJ4hxL8JZWibhVkuE6hVsOB8BeMi8nm", false)
    );

    private static Document user(String id, String passwordHash, boolean admin) {
        return new Document("_id", id)
                .append("member_name", id)
                .append("bio", "")
                .append("topics", List.of())
                .append("event_count", 0)
                .append("group_count", 0)
                .append("upcoming_events", List.of())
                .append("password", passwordHash)
                .append("isAdmin", admin)
                .append("_class", "com.example.joinUs.model.mongodb.User");
    }

    private static final GenericContainer mongodb = new GenericContainer(MONGODB_IMAGE)
            .withExposedPorts(MONGODB_PORT);

    private static final GenericContainer neo4j = new GenericContainer(NEO4J_IMAGE)
            .withEnv("NEO4J_AUTH", NEO4J_USERNAME + "/" + NEO4J_PASSWORD)
            .withExposedPorts(NEO4J_BOLT_PORT)
            .waitingFor(Wait.forLogMessage(".*Started.*", 1))
            .withStartupTimeout(Duration.ofMinutes(2));

    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/joinus/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/joinus-sut.jar";
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

    private Driver neo4jDriver;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/joinus-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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

    private String mongodbUri() {
        return "mongodb://" + mongodb.getHost() + ":" + mongodb.getMappedPort(MONGODB_PORT) + "/" + MONGODB_DATABASE;
    }

    private String neo4jUri() {
        return "bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--spring.mongodb.uri=" + mongodbUri(),
                "--spring.neo4j.uri=" + neo4jUri(),
                "--spring.neo4j.authentication.username=" + NEO4J_USERNAME,
                "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD,
                // application.properties logs every DB command at DEBUG
                "--logging.level.org.mongodb.driver=INFO",
                "--logging.level.org.neo4j.driver=INFO",
                "--logging.level.org.springframework.security=INFO"
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
        return "Started JoinUsApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        mongodb.start();
        neo4j.start();
        mongoClient = MongoClients.create(mongodbUri());
        neo4jDriver = GraphDatabase.driver(neo4jUri(), AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));
        seedUsers();
    }

    private void seedUsers() {
        mongoClient.getDatabase(MONGODB_DATABASE).getCollection("members").insertMany(USERS);
        try (Session session = neo4jDriver.session()) {
            session.run("UNWIND $ids AS id CREATE (:Member {member_id: id, member_name: id})",
                    Map.of("ids", USERS.stream().map(u -> u.getString("_id")).toList())).consume();
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
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        if (neo4jDriver != null) {
            neo4jDriver.close();
            neo4jDriver = null;
        }
        neo4j.stop();
        mongodb.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.joinUs.";
    }

    // Reseeding also undoes DELETE /user/profile, which removes the logged-in account. Black-box runs
    // have no reset, so there that endpoint has to be excluded (--endpointExclude /user/profile).
    @Override
    public void resetStateOfSUT() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            db.getCollection(name).deleteMany(new Document());
        }
        try (Session session = neo4jDriver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
        seedUsers();
    }

    // Lets EvoMaster compute its Mongo query heuristics; without it every test that touches a
    // Mongo endpoint fails coverage retrieval with an NPE and forces a full SUT restart.
    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }


    // POST /login takes a JSON body and answers with a JSESSIONID cookie
    private static AuthenticationDto cookieLogin(String name, String userId, String password) {
        LoginEndpoint login = new LoginEndpoint();
        login.setEndpoint("/login");
        login.setVerb(LoginEndpoint.HttpVerb.POST);
        login.setContentType("application/json");
        login.setPayloadRaw("{\"userid\":\"" + userId + "\",\"password\":\"" + password + "\"}");
        login.setExpectCookies(true);
        AuthenticationDto dto = new AuthenticationDto(name);
        dto.setLoginEndpointAuth(login);
        return dto;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of(
                cookieLogin("WfdAdmin", "wfd_admin", "Wfd-Pass1"),
                cookieLogin("WfdUser", "wfd_user", "Wfd-Pass2")
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
