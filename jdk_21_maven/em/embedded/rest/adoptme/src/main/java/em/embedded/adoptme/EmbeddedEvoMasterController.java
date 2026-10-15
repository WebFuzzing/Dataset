package em.embedded.adoptme;

import com.programacion3.adoptme.AdoptMApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
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
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

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

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private ConfigurableApplicationContext ctx;

    /** Driver over the same database the SUT uses, for EvoMaster to read the graph and insert test data. */
    private Driver neo4jDriver;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        neo4j.start();

        ctx = SpringApplication.run(AdoptMApplication.class, new String[]{
                "--server.port=0",
                "--spring.neo4j.uri=bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT),
                "--spring.neo4j.authentication.username=" + NEO4J_USER,
                "--spring.neo4j.authentication.password=" + NEO4J_PASSWORD
        });

        runCypher(BACKUP_SEEDED_ADOPTERS);

        neo4jDriver = GraphDatabase.driver(
                "bolt://" + neo4j.getHost() + ":" + neo4j.getMappedPort(NEO4J_BOLT_PORT),
                AuthTokens.basic(NEO4J_USER, NEO4J_PASSWORD));

        return "http://localhost:" + getSutPort();
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

        if (neo4jDriver != null) {
            neo4jDriver.close();
            neo4jDriver = null;
        }

        neo4j.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.programacion3.adoptme.";
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


    /** Lets EvoMaster read the graph for its Neo4j heuristics and insert test data into it. */
    @Override
    public ReflectionBasedNeo4jClient getNeo4jConnection() {
        return neo4jDriver == null ? null : new ReflectionBasedNeo4jClient(neo4jDriver);
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }


    /*
        /routes/tsp/bnb is factorial in its "nodes" (default = all 15 shelters) and never returns;
        Black-box runs need excludes endpoints.
     */
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
