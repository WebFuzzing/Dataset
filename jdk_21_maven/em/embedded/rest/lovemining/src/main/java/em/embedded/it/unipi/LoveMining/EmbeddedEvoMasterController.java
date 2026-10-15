package em.embedded.it.unipi.LoveMining;

import it.unipi.LoveMining.LoveMiningApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.neo4j.ReflectionBasedNeo4jClient;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import com.webfuzzing.commons.auth.Header;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "LoveMining";

    private static final String NEO4J_IMAGE = "neo4j:5.26";

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final String NEO4J_USERNAME = "neo4j";

    private static final String NEO4J_PASSWORD = "wfdNeo4jPass123";

    private static final String STATE = "tuscany";

    private static final String ADMIN1_HASH = "$2a$10$Q5Uui.PMfXK0RjKqJek4Cusq5tKmTeUXwm6tdD.HA6LrLew7JK0me";

    private static final String ADMIN2_HASH = "$2a$10$Zbu3QxMgPmo1ZunqPxn2SeWuEGJP9RHsBeoCOoKdlfuF8BNn0AnFy";

    private static final String USER1_HASH = "$2a$10$3gCReephTyeV2WcmQklcJuEjALyr.sTkTyUKuVmCX7fFyPSAYPFp6";

    private static final String USER2_HASH = "$2a$10$t1QO4YBNJaM5gQk6v9I9EeiBjHO.1EWmKcURu8/NfMaaNrpvRIC3e";

    // seed
    private static final List<Document> USERS = List.of(
            account("wfd_admin1", ADMIN1_HASH, true),
            account("wfd_admin2", ADMIN2_HASH, true),
            profile("wfd_user1", USER1_HASH, 28, "m", "straight", "single", "pisa",
                    "I love music and travel.", List.of("music", "travel")),
            profile("wfd_user2", USER2_HASH, 27, "f", "straight", "single", "pisa",
                    "Music and art are my life.", List.of("music", "art"))
    );

    private static Document account(String id, String hash, boolean admin) {
        return new Document("_id", id)
                .append("Email", id + "@wfd.invalid")
                .append("Password", hash)
                .append("is_admin", admin);
    }

    private static Document profile(String id, String hash, int age, String sex, String orientation,
                                    String status, String city, String essay0, List<String> interests) {
        return account(id, hash, false)
                .append("age", age)
                .append("sex", sex)
                .append("orientation", orientation)
                .append("status", status)
                .append("city", city)
                .append("state", STATE)
                .append("essay0", essay0)
                .append("interests", interests);
    }

    // Dates must stay relative: the glow-up aggregation groups reviews around a 6-month cutoff
    private static Document review(String id, String targetId, int rating, String comment, int monthsAgo) {
        Date date = Date.from(LocalDateTime.now().minusMonths(monthsAgo).atZone(ZoneId.systemDefault()).toInstant());
        return new Document("_id", id)
                .append("target_id", targetId)
                .append("rating", rating)
                .append("comment", comment)
                .append("review_date", date);
    }

    // Reviews record no author: ReviewDocument has no such field, the link lives in
    // UserDocument.reviews_made. Three on one target, spanning the cutoff, is the glow-up minimum.
    private static List<Document> reviews() {
        return List.of(
                review("wfd_rv1", "wfd_user1", 2, "Conversation was hard work.", 9),
                review("wfd_rv2", "wfd_user1", 1, "Not my type at all.", 8),
                review("wfd_rv3", "wfd_user1", 5, "Much better than I expected.", 1)
        );
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
        seed();

        ctx = SpringApplication.run(LoveMiningApplication.class, new String[]{
                "--server.port=0",
                "--spring.data.mongodb.uri=" + mongodbUri(),
                "--spring.neo4j.uri=" + neo4jUri(),
                "--spring.neo4j.authentication.username=" + NEO4J_USERNAME,
                "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD
        });

        return "http://localhost:" + getSutPort();
    }

    private void seed() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        List<Document> users = new ArrayList<>();
        for (Document u : USERS) {
            users.add(new Document(u));
        }
        db.getCollection("users").insertMany(users);
        db.getCollection("reviews").insertMany(reviews());

        try (Session session = neo4jDriver.session()) {
            session.run("""
                            MERGE (s:State {name: $state})
                            WITH s
                            UNWIND $rows AS row
                            MERGE (c:City {name: row.city})
                            MERGE (c)-[:LOCATED_IN]->(s)
                            CREATE (u:User {_id: row.id, age: row.age, sex: row.sex, orientation: row.orientation})
                            CREATE (u)-[:LIVES_IN]->(c)
                            WITH u, row
                            UNWIND row.interests AS name
                            MERGE (i:Interest {name: name})
                            CREATE (u)-[:HAS_INTEREST]->(i)
                            """,
                    Map.of("state", STATE, "rows", graphRows())).consume();
        }
    }

    // Admins have no profile, so they get no graph node
    private static List<Map<String, Object>> graphRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Document u : USERS) {
            if (Boolean.TRUE.equals(u.getBoolean("is_admin"))) {
                continue;
            }
            rows.add(Map.of(
                    "id", u.getString("_id"),
                    "age", u.getInteger("age"),
                    "sex", u.getString("sex"),
                    "orientation", u.getString("orientation"),
                    "city", u.getString("city"),
                    "interests", u.getList("interests", String.class)));
        }
        return rows;
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
        return "it.unipi.LoveMining.";
    }

    // Undoes the two endpoints that can break authentication: PATCH /api/users/me overwrites the
    // password of the logged-in account, DELETE /api/admin/users/{id} removes it. Black-box runs
    // have no reset, so PATCH /api/users/me has to be excluded there.
    @Override
    public void resetStateOfSUT() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            db.getCollection(name).deleteMany(new Document());
        }
        try (Session session = neo4jDriver.session()) {
            session.run("MATCH (n) DETACH DELETE n").consume();
        }
        seed();
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

    /** Lets EvoMaster read the graph for its Neo4j heuristics and insert test data into it. */
    @Override
    public ReflectionBasedNeo4jClient getNeo4jConnection() {
        return neo4jDriver == null ? null : new ReflectionBasedNeo4jClient(neo4jDriver);
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    // Login name is the email: CustomUserDetailsService looks accounts up by it
    private static AuthenticationDto basicAuth(String name, String username, String password) {
        AuthenticationDto dto = new AuthenticationDto(name);
        String token = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        Header header = new Header();
        header.setName("Authorization");
        header.setValue("Basic " + token);
        dto.setFixedHeaders(List.of(header));
        return dto;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of(
                basicAuth("WfdAdmin1", "wfd_admin1@wfd.invalid", "Wfd-Admin-Pass1"),
                basicAuth("WfdAdmin2", "wfd_admin2@wfd.invalid", "Wfd-Admin-Pass2"),
                basicAuth("WfdUser1", "wfd_user1@wfd.invalid", "Wfd-User-Pass1"),
                basicAuth("WfdUser2", "wfd_user2@wfd.invalid", "Wfd-User-Pass2")
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
