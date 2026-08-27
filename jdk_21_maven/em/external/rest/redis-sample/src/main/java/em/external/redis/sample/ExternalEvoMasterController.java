package em.external.redis.sample;


import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String REDIS_VERSION = "7.4-alpine";

    private static final int REDIS_PORT = 6379;

    private static final String REDIS_PASSWORD = "53cret";

    private static final String SECURITY_EXCLUDE =
            "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration";

    private static final int NUMBER_OF_RATINGS = 5000;

    private static final int RATING_STARS = 5;

    private static final GenericContainer redis = new GenericContainer("redis:" + REDIS_VERSION)
            .withCommand("redis-server", "--requirepass", REDIS_PASSWORD)
            .withExposedPorts(REDIS_PORT);


    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/redis-sample/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/redis-sample-sut.jar";
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

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/redis-sample-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
                "--spring.data.redis.host=" + redis.getHost(),
                "--spring.data.redis.port=" + redis.getMappedPort(REDIS_PORT),
                "--spring.data.redis.password=" + REDIS_PASSWORD,
                "--spring.autoconfigure.exclude=" + SECURITY_EXCLUDE,
                "--app.numberOfRatings=" + NUMBER_OF_RATINGS,
                "--app.ratingStars=" + RATING_STARS
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

    /*
        Spring logs "Started ... in" before the CommandLineRunners, and seeding the ratings takes
        another ~12s. Fuzzing that early makes EvoMaster's kill switch abort the still-running
        runner, which brings the whole SUT down. This is the last line the last runner writes.
     */
    @Override
    public String getLogMessageOfInitializedServer() {
        return ">>>> BookRating created...";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        redis.start();
    }

    @Override
    public void postStart() {
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        redis.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "id.my.hendisantika.springbootredissample.";
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
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

}
