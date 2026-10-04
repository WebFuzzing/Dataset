package em.embedded.kafkapublisher;

import com.ivanfranchin.publisherapi.PublisherApiApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String ELASTICSEARCH_VERSION = "9.2.1";

    private static final int ELASTICSEARCH_PORT = 9200;

    private static final GenericContainer elasticsearch = new GenericContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION)
            .withEnv("discovery.type", "single-node")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
            .withTmpFs(Collections.singletonMap("/usr/share/elasticsearch/data", "rw"))
            .withExposedPorts(ELASTICSEARCH_PORT)
            .waitingFor(Wait.forHttp("/").forPort(ELASTICSEARCH_PORT).forStatusCode(200))
            .withStartupTimeout(Duration.ofMinutes(3));

    // Same mapping collector-service creates through Spring Data's @Field annotations
    private static final String INDEX_MAPPINGS = "{\"properties\":{"
            + "\"title\":{\"type\":\"text\",\"analyzer\":\"my_analyzer\",\"search_analyzer\":\"my_search_analyzer\"},"
            + "\"text\":{\"type\":\"text\",\"analyzer\":\"my_analyzer\",\"search_analyzer\":\"my_search_analyzer\"},"
            + "\"category\":{\"type\":\"text\",\"analyzer\":\"my_analyzer\",\"search_analyzer\":\"my_search_analyzer\"},"
            + "\"datetime\":{\"type\":\"date\"}}}";

    // The API is read-only: without seed data every read is empty. One news per category of categorizer-service.
    private static final String SEED_BULK =
            news("8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a01", "Brazil wins the cup", "The final in Brasilia ended two to one", "Sport", "2026-01-10T09:00:00Z")
            + news("8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a02", "Summit in Geneva", "World leaders meet to discuss trade", "World", "2026-01-11T10:30:00Z")
            + news("8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a03", "Mars rover finds water", "New data from the rover confirms ice", "Science", "2026-01-12T12:15:00Z")
            + news("8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a04", "Film festival opens", "Directors from forty countries arrive", "Entertainment", "2026-01-13T18:45:00Z")
            + news("8b6e3c1a-5f2d-4e7b-9a10-1c2d3e4f5a05", "New vaccine approved", "Health authorities approve the vaccine", "Health", "2026-01-14T07:20:00Z");

    private static String news(String id, String title, String text, String category, String datetime) {
        return "{\"index\":{\"_index\":\"news\",\"_id\":\"" + id + "\"}}\n"
                + "{\"id\":\"" + id + "\",\"title\":\"" + title + "\",\"text\":\"" + text
                + "\",\"category\":\"" + category + "\",\"datetime\":\"" + datetime + "\"}\n";
    }


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

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        elasticsearch.start();
        seedElasticsearch();

        // Eureka and Zipkin only serve the demo's infrastructure, not the API
        ctx = SpringApplication.run(PublisherApiApplication.class, new String[]{
                "--server.port=0",
                "--ELASTICSEARCH_HOST=" + elasticsearch.getHost(),
                "--ELASTICSEARCH_REST_PORT=" + elasticsearch.getMappedPort(ELASTICSEARCH_PORT),
                "--eureka.client.enabled=false",
                "--management.tracing.export.zipkin.enabled=false"
        });

        return "http://localhost:" + getSutPort();
    }

    private void seedElasticsearch() {
        String settings = new Scanner(getClass().getResourceAsStream("/news-es-analysis.json"), "UTF-8")
                .useDelimiter("\\A").next();
        curl("PUT", "/news", "{\"settings\":" + settings + ",\"mappings\":" + INDEX_MAPPINGS + "}");
        String result = curl("POST", "/_bulk?refresh=true", SEED_BULK);
        if (!result.contains("\"errors\":false")) {
            throw new RuntimeException("Failed to seed Elasticsearch: " + result);
        }
    }

    // The container's own curl, so the driver needs no Elasticsearch client
    private String curl(String method, String path, String body) {
        try {
            Container.ExecResult r = elasticsearch.execInContainer("curl", "-sS", "-f", "-X", method,
                    "http://localhost:" + ELASTICSEARCH_PORT + path,
                    "-H", "Content-Type: application/json", "--data-binary", body);
            if (r.getExitCode() != 0) {
                throw new RuntimeException("curl " + method + " " + path + " failed: " + r.getStderr() + r.getStdout());
            }
            return r.getStdout();
        } catch (Exception e) {
            throw new RuntimeException(e);
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

        elasticsearch.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.ivanfranchin.";
    }

    // The API has no write endpoint, so the seed is never modified
    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
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
