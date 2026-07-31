package em.embedded.com.hendisantika;

import com.hendisantika.SpringBootRedisSampleApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;

public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final int REDIS_PORT = 6379;

    private final GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private ConfigurableApplicationContext ctx;
    private String redisHost;
    private int redisPort;

    public static void main(String[] args) {
        int port = 40100;
        if (args.length > 0) port = Integer.parseInt(args[0]);
        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    public EmbeddedEvoMasterController() { this(40100); }

    public EmbeddedEvoMasterController(int port) { setControllerPort(port); }

    @Override
    public String startSut() {
        redis.start();
        redisHost = redis.getHost();
        redisPort = redis.getMappedPort(REDIS_PORT);

        ctx = new SpringApplicationBuilder(SpringBootRedisSampleApplication.class)
                .properties(
                        "--server.port=0",
                        "spring.data.redis.host=" + redisHost,
                        "spring.data.redis.port=" + redisPort
                ).run();

        return "http://localhost:" + getSutPort();
    }

    @Override
    public void stopSut() {
        if (ctx != null) { ctx.stop(); ctx.close(); }
        if (redis.isRunning()) redis.stop();
    }

    @Override
    public void resetStateOfSUT() {
        try (ReflectionBasedRedisClient client =
                     new ReflectionBasedRedisClient(redisHost, redisPort, 0)) {
            client.flushAll();
        }
    }

    @Override
    public ReflectionBasedRedisClient getRedisConnection() {
        return new ReflectionBasedRedisClient(redisHost, redisPort, 0);
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.hendisantika.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v3/api-docs", null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() { return null; }

    @Override
    public List<DbSpecification> getDbSpecifications() { return null; }

    protected int getSutPort() {
        return (Integer)((Map) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }
}