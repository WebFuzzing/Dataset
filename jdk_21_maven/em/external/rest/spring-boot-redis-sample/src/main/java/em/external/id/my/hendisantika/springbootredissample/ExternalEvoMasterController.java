package em.external.id.my.hendisantika.springbootredissample;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient;
import org.evomaster.client.java.sql.DbSpecification;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;
    private static final int DEFAULT_SUT_PORT = 12345;
    private static final int REDIS_PORT = 6379;

    private final GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private String redisHost;
    private int redisPort;

    public static void main(String[] args) {
        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) controllerPort = Integer.parseInt(args[0]);

        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) sutPort = Integer.parseInt(args[1]);

        String jarLocation = "cs/rest/spring-boot-redis-sample/target";
        if (args.length > 2) jarLocation = args[2];
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/spring-boot-redis-sample-sut.jar";
        }

        int timeoutSeconds = 120;
        if (args.length > 3) timeoutSeconds = Integer.parseInt(args[3]);

        String command = "java";
        if (args.length > 4) command = args[4];

        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);
        starter.start();
    }

    private final int sutPort;
    private final int timeoutSeconds;
    private String jarLocation;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/spring-boot-redis-sample-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
                "--spring.data.redis.host=" + redisHost,
                "--spring.data.redis.port=" + redisPort
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
        return "Started SpringBootRedisSampleApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        redis.start();
        redisHost = redis.getHost();
        redisPort = redis.getMappedPort(REDIS_PORT);
    }

    @Override
    public void postStart() {}

    @Override
    public void preStop() {}

    @Override
    public void postStop() {
        if (redis.isRunning()) redis.stop();
    }

    @Override
    public void resetStateOfSUT() {
        ReflectionBasedRedisClient client = new ReflectionBasedRedisClient(redisHost, redisPort, 0);
        try {
            client.flushAll();
        } finally {
            client.close();
        }
    }

    @Override
    public ReflectionBasedRedisClient getRedisConnection() {
        return new ReflectionBasedRedisClient(redisHost, redisPort, 0);
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "id.my.hendisantika.";
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

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }
}