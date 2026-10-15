package em.external.adoptme;


import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.neo4j.ReflectionBasedNeo4jClient;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String NEO4J_VERSION = "5.26";

    private static final int NEO4J_BOLT_PORT = 7687;

    private static final int NEO4J_HTTP_PORT = 7474;

    private static final String NEO4J_USER = "neo4j";

    private static final String NEO4J_PASSWORD = "neo4j123";

    private static final String AUTH_HEADER = "Basic " + Base64.getEncoder().encodeToString(
            (NEO4J_USER + ":" + NEO4J_PASSWORD).getBytes(StandardCharsets.UTF_8));

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /*
        The SUT seeds the graph itself at startup, so the seed is copied out of the database
        instead of being duplicated here: one shadow node per adopter, with the same properties.
     */
    private static final String BACKUP_SEEDED_ADOPTERS =
            "MATCH (a:Adopter) CREATE (b:SeedBackup) SET b = properties(a)";

    private static final String DELETE_ADDED_ADOPTERS =
            "MATCH (a:Adopter) WHERE NOT EXISTS { MATCH (:SeedBackup {id: a.id}) } DETACH DELETE a";

    private static final String RESTORE_SEEDED_ADOPTERS =
            "MATCH (b:SeedBackup) MERGE (a:Adopter {id: b.id}) SET a = properties(b)";

    private static final GenericContainer neo4j = new GenericContainer("neo4j:" + NEO4J_VERSION)
            .withEnv("NEO4J_AUTH", NEO4J_USER + "/" + NEO4J_PASSWORD)
            .withEnv("NEO4J_server_memory_heap_max__size", "512M")
            .withEnv("NEO4J_server_memory_pagecache_size", "256M")
            .withExposedPorts(NEO4J_BOLT_PORT, NEO4J_HTTP_PORT)
            .waitingFor(Wait.forLogMessage(".*Started\\..*", 1))
            .withStartupTimeout(Duration.ofMinutes(3));


    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/adoptme/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/adoptme-sut.jar";
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

    /** Driver over the same database the SUT uses, for EvoMaster to read the graph and insert test data. */
    private Driver neo4jDriver;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/adoptme-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
                "--spring.neo4j.uri=bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT),
                "--spring.neo4j.authentication.username=" + NEO4J_USER,
                "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{
                /*
                    /transport/optimal-dp allocates a DP table of size 41 x capacityKg, so a large
                    capacityKg must fail fast instead of dragging the JVM through a long GC spiral.
                 */
                "-Xmx1G"
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
        return "===== DATABASE SEEDED SUCCESSFULLY =====";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        neo4j.start();
        neo4jDriver = GraphDatabase.driver(
                "bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT),
                AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));
    }

    @Override
    public void postStart() {
        runCypher(BACKUP_SEEDED_ADOPTERS);
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        if (neo4jDriver != null) {
            neo4jDriver.close();
            neo4jDriver = null;
        }
        neo4j.stop();
    }

    /** Lets EvoMaster read the graph for its Neo4j heuristics and insert test data into it. */
    @Override
    public ReflectionBasedNeo4jClient getNeo4jConnection() {
        return neo4jDriver == null ? null : new ReflectionBasedNeo4jClient(neo4jDriver);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.programacion3.adoptme.";
    }

    /*
        /routes/tsp/bnb is factorial in its "nodes" (default = all 15 shelters) and never returns;
        Black-box runs need excludes endpoints.
     */
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

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    /*
        POST /adopters is the only endpoint that writes: it can add adopters, and it overwrites a
        seeded one when it reuses its id. The shelters, dogs and relationships are read-only.
     */
    @Override
    public void resetStateOfSUT() {
        if (neo4j.isRunning()) {
            runCypher(DELETE_ADDED_ADOPTERS, RESTORE_SEEDED_ADOPTERS);
        }
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    /** Sends the given Cypher statements to Neo4j as a single transaction, over its HTTP API. */
    private void runCypher(String... statements) {
        String body = Arrays.stream(statements)
                .map(s -> "{\"statement\":\"" + s + "\"}")
                .collect(Collectors.joining(",", "{\"statements\":[", "]}"));
        String url = "http://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_HTTP_PORT)
                + "/db/neo4j/tx/commit";

        HttpResponse<String> response;
        try {
            response = HTTP.send(HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .header("Authorization", AUTH_HEADER)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException("Could not run " + body, e);
        }
        // a rejected statement comes back as 200 with the error in the body, so check both
        if (response.statusCode() != 200 || !response.body().contains("\"errors\":[]")) {
            throw new IllegalStateException("Neo4j rejected " + body + ": " + response.body());
        }
    }

}
