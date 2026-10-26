package em.external.uk.gov.pay.api.app;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    public static void main(String[] args) {

        int controllerPort = 40100;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = 12345;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/pay-publicapi/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/pay-publicapi-sut.jar";
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
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int timeoutSeconds;
    private final int sutPort;

    private String jarLocation;
    private final String CONFIG_FILE = "em_config.yaml";

    private static final int REDIS_PORT = 6379;

    private static final String REDIS_VERSION = "7.2.3";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer redisContainer = new GenericContainer("redis:" + REDIS_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(REDIS_PORT);

    private static String REDIS_URL = "";

    private static JedisPool jedisPool;

    public ExternalEvoMasterController() {
        this(40100, "../api/target", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(int controllerPort,
                                       String jarLocation,
                                       int sutPort,
                                       int timeoutSeconds,
                                       String command) {
        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
        setJavaCommand(command);
        createConfigurationFile();
    }


    /**
     * Unfortunately, it seems like Dropwizard is buggy, and has
     * problems with overriding params without a YML file :(
     */
    private void createConfigurationFile() {

        //save config to same folder of JAR file
        Path path = getConfigPath();

        try (InputStream is = this.getClass().getResourceAsStream("/" + CONFIG_FILE)) {
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Path getConfigPath() {
        return Paths.get(jarLocation)
                .toAbsolutePath()
                .getParent()
                .resolve(CONFIG_FILE)
                .normalize();
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{"server", getConfigPath().toAbsolutePath().toString()};
    }

    @Override
    public String[] getJVMParameters() {

        return new String[]{
                "-Ddw.server.applicationConnectors[0].port=" + sutPort,
                "-Ddw.server.adminConnectors[0].port=0",
                "-Ddw.redis.endpoint=" + REDIS_URL
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
        return "Started application";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        redisContainer.start();

        REDIS_URL = redisContainer.getHost() + redisContainer.getMappedPort(REDIS_PORT);

        jedisPool = new JedisPool(redisContainer.getHost(), redisContainer.getMappedPort(REDIS_PORT));
    }

    @Override
    public void postStart() {

        resetStateOfSUT();
    }

    @Override
    public void preStop() {

    }

    @Override
    public void postStop() {
        redisContainer.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "uk.gov.pay.api.";
    }

    @Override
    public void resetStateOfSUT() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/assets/publicapi_spec.json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        AuthenticationDto dto = AuthUtils.getForAuthorizationHeader("foo", "Bearer asdfghdasdjlguuolnga94upq3nrd2642sq7uel0oo");
        dto.setRequireMockHandling(true);
        return Arrays.asList(dto);
    }


    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }
}
