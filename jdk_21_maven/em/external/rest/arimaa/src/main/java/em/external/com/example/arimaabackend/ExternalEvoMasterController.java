package em.external.com.example.arimaabackend;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import com.webfuzzing.commons.auth.Header;
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
import org.testcontainers.utility.MountableFile;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String MYSQL_IMAGE = "mysql:9.5.0";

    private static final int MYSQL_PORT = 3306;

    // Created by the SUT's own Database/mysql/Sql_arimaa_10_init.sql, granted full privileges
    // on this exact (hardcoded, not env-driven) schema name.
    private static final String MYSQL_DATABASE = "arimaadockermysqldb";

    private static final String MYSQL_APP_USER = "admin";

    private static final String MYSQL_APP_PASSWORD = "AdminPassword123!";

    private static final String MYSQL_ROOT_PASSWORD = "wfdRootPass123!";

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "arimaadb";

    private static final String NEO4J_IMAGE = "neo4j:5.26";

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final String NEO4J_USERNAME = "neo4j";

    private static final String NEO4J_PASSWORD = "wfdNeo4jPass123";

    // Two known-credential ADMIN accounts (see Database/mysql/Sql_arimaa_62b_default_user.sql)
    private static final String INIT_SQL =
            "INSERT IGNORE INTO Users (username, email, password, role, created_at, updated_at) VALUES "
                    + "('admin1', 'admin1@example.com', '$2a$10$a7Dii8pcWQMclYxLt9Kb1eWpbRNbAPTsMRlJkm7ZT.wYIemq4oiBi', 'ADMIN', NOW(), NOW()), "
                    + "('user1', 'user1@example.com', '$2a$10$DK1T8LJLPBcPLWhm7i/L1esnux0b7mV0HjMbB02CL794blj0M0lYG', 'USER', NOW(), NOW()), "
                    + "('wfd_admin1', 'wfd_admin1@example.com', '$2a$10$3mawxiylgOPZOvvJO24bxeuUcQyQf.n53SzzAX9fBNiT7bfzZn4WO', 'ADMIN', NOW(), NOW()), "
                    + "('wfd_admin2', 'wfd_admin2@example.com', '$2a$10$8h.Zwu6RHMf0VZsEIZ2rcewbF4CU/nLpzoSSPjE1M1D.nof/9ZQVy', 'ADMIN', NOW(), NOW());";

    private static final GenericContainer mysql = new GenericContainer(MYSQL_IMAGE)
            .withEnv("MYSQL_DATABASE", MYSQL_DATABASE)
            .withEnv("MYSQL_ROOT_PASSWORD", MYSQL_ROOT_PASSWORD)
            .withCopyFileToContainer(MountableFile.forHostPath("cs/rest/arimaa/Database/mysql"), "/docker-entrypoint-initdb.d")
            .withExposedPorts(MYSQL_PORT)
            // First "ready for connections" is the temporary bootstrap server that runs the
            // initdb.d scripts; the second is the real server, only up once they have finished.
            .waitingFor(Wait.forLogMessage(".*ready for connections.*", 2))
            .withStartupTimeout(Duration.ofMinutes(3));

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
        String jarLocation = "cs/rest/arimaa/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/arimaa-sut.jar";
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

    private MongoClient mongoClient;

    private Driver neo4jDriver;

    private Map<String, List<Document>> mongoSnapshot;

    private List<Map<String, Object>> neo4jUserSnapshot;

    private List<Map<String, Object>> neo4jHasUserSnapshot;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/arimaa-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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

    private String mysqlJdbcUrl() {
        return "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(MYSQL_PORT) + "/" + MYSQL_DATABASE;
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
                "--spring.datasource.url=" + mysqlJdbcUrl(),
                "--spring.datasource.username=" + MYSQL_APP_USER,
                "--spring.datasource.password=" + MYSQL_APP_PASSWORD,
                "--spring.mongodb.uri=" + mongodbUri(),
                "--spring.neo4j.uri=" + neo4jUri(),
                "--spring.neo4j.authentication.username=" + NEO4J_USERNAME,
                "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD
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
        return "Started ArimaaBackendApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        mysql.start();
        mongodb.start();
        neo4j.start();
        runMigration();

        mongoClient = MongoClients.create(mongodbUri());
        neo4jDriver = GraphDatabase.driver(neo4jUri(), AuthTokens.basic(NEO4J_USERNAME, NEO4J_PASSWORD));
        captureMongoSnapshot();
        captureNeo4jUserSnapshot();
    }

    private void captureMongoSnapshot() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        mongoSnapshot = new HashMap<>();
        for (String name : db.listCollectionNames()) {
            List<Document> docs = new ArrayList<>();
            db.getCollection(name).find().into(docs);
            mongoSnapshot.put(name, docs);
        }
    }

    private void captureNeo4jUserSnapshot() {
        try (Session session = neo4jDriver.session()) {
            neo4jUserSnapshot = session.run("MATCH (u:User) RETURN properties(u) AS props")
                    .list(r -> r.get("props").asMap());
            neo4jHasUserSnapshot = session.run(
                            "MATCH (p:Player)-[:HAS_USER]->(u:User) RETURN p.id AS playerId, u.id AS userId")
                    .list(r -> Map.of("playerId", r.get("playerId").asObject(), "userId", r.get("userId").asObject()));
        }
    }

    // The Mongo/Neo4j controllers only serve data once it has been copied over from MySQL by the
    // SUT's own "migration" profile (com.example.arimaabackend.migration.MigrationRunner), which
    // calls System.exit() when done. It must run as its own process, before the SUT itself starts.
    private void runMigration() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-jar", jarLocation,
                    "--spring.profiles.active=migration",
                    "--spring.datasource.url=" + mysqlJdbcUrl(),
                    "--spring.datasource.username=" + MYSQL_APP_USER,
                    "--spring.datasource.password=" + MYSQL_APP_PASSWORD,
                    "--spring.mongodb.uri=" + mongodbUri(),
                    "--spring.neo4j.uri=" + neo4jUri(),
                    "--spring.neo4j.authentication.username=" + NEO4J_USERNAME,
                    "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD
            );
            pb.inheritIO();
            Process process = pb.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                System.err.println("arimaa migration step timed out, killed");
            } else if (process.exitValue() != 0) {
                System.err.println("arimaa migration step exited with code " + process.exitValue());
            }
        } catch (Exception e) {
            System.err.println("arimaa migration step failed: " + e.getMessage());
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
        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (Exception e) {
                // ignore
            }
            sqlConnection = null;
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
        mysql.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.arimaabackend.";
    }

    @Override
    public void resetStateOfSUT() {
        resetMongo();
        resetNeo4jUsers();
    }

    private void resetMongo() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (Map.Entry<String, List<Document>> entry : mongoSnapshot.entrySet()) {
            var collection = db.getCollection(entry.getKey());
            collection.deleteMany(new Document());
            if (!entry.getValue().isEmpty()) {
                List<Document> copies = new ArrayList<>();
                for (Document d : entry.getValue()) {
                    copies.add(new Document(d));
                }
                collection.insertMany(copies);
            }
        }
    }

    private void resetNeo4jUsers() {
        try (Session session = neo4jDriver.session()) {
            session.run("MATCH (u:User) DETACH DELETE u").consume();
            if (!neo4jUserSnapshot.isEmpty()) {
                session.run("UNWIND $rows AS row CREATE (u:User) SET u = row",
                        Map.of("rows", neo4jUserSnapshot)).consume();
            }
            if (!neo4jHasUserSnapshot.isEmpty()) {
                session.run("""
                                UNWIND $rows AS row
                                MATCH (p:Player {id: row.playerId}), (u:User {id: row.userId})
                                MERGE (p)-[:HAS_USER]->(u)
                                """,
                        Map.of("rows", neo4jHasUserSnapshot)).consume();
            }
        }
    }

    // Lets EvoMaster compute its Mongo query heuristics; without it every test that touches a
    // Mongo endpoint fails coverage retrieval with an NPE and forces a full SUT restart.
    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        try {
            if (sqlConnection == null || sqlConnection.isClosed()) {
                sqlConnection = DriverManager.getConnection(mysqlJdbcUrl(), MYSQL_APP_USER, MYSQL_APP_PASSWORD);
            }
            return List.of(new DbSpecification(DatabaseType.MYSQL, sqlConnection)
                    .withInitSqlScript(INIT_SQL));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


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
                basicAuth("WfdAdmin1", "wfd_admin1", "Wfd-Admin-Pass1"),
                basicAuth("WfdAdmin2", "wfd_admin2", "Wfd-Admin-Pass2")
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
