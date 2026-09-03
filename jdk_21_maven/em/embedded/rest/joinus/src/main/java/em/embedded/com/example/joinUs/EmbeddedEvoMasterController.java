package em.embedded.com.example.joinUs;

import com.example.joinUs.JoinUsApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
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
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

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

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private ConfigurableApplicationContext ctx;

    private MongoClient mongoClient;

    private Driver neo4jDriver;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    private String mongodbUri() {
        return "mongodb://" + mongodb.getHost() + ":" + mongodb.getMappedPort(MONGODB_PORT) + "/" + MONGODB_DATABASE;
    }

    private String neo4jUri() {
        return "bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT);
    }

    @Override
    public String startSut() {

        mongodb.start();
        neo4j.start();
        mongoClient = MongoClients.create(mongodbUri());
        neo4jDriver = GraphDatabase.driver(neo4jUri(), AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));
        seedUsers();

        ctx = SpringApplication.run(JoinUsApplication.class, new String[]{
                "--server.port=0",
                "--spring.mongodb.uri=" + mongodbUri(),
                "--spring.neo4j.uri=" + neo4jUri(),
                "--spring.neo4j.authentication.username=" + NEO4J_USERNAME,
                "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD,
                // application.properties logs every DB command at DEBUG
                "--logging.level.org.mongodb.driver=INFO",
                "--logging.level.org.neo4j.driver=INFO",
                "--logging.level.org.springframework.security=INFO"
        });

        return "http://localhost:" + getSutPort();
    }

    private void seedUsers() {
        mongoClient.getDatabase(MONGODB_DATABASE).getCollection("members").insertMany(USERS);
        try (Session session = neo4jDriver.session()) {
            session.run("UNWIND $ids AS id CREATE (:Member {member_id: id, member_name: id})",
                    Map.of("ids", USERS.stream().map(u -> u.getString("_id")).toList())).consume();
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
        if (ctx != null) {
            ctx.stop();
            ctx.close();
            ctx = null;
        }
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
                "http://localhost:" + getSutPort() + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
