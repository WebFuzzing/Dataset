package em.external.kafkaproducer;


import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String KAFKA_VERSION = "3.8.1";

    private static final int KAFKA_PORT = 9092;

    private static final KafkaContainer kafka = new KafkaContainer("apache/kafka:" + KAFKA_VERSION);


    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/kafka-producer-publisher/producer-api/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/kafka-producer-sut.jar";
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
        this(DEFAULT_CONTROLLER_PORT, "../target/kafka-producer-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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

    // Eureka and Zipkin only serve the demo's infrastructure, not the API
    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--KAFKA_HOST=" + kafka.getHost(),
                "--KAFKA_PORT=" + kafka.getMappedPort(KAFKA_PORT),
                "--eureka.client.enabled=false",
                "--management.tracing.export.zipkin.enabled=false"
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
        return "Started ProducerApiApplication in";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        kafka.start();
    }

    @Override
    public void postStart() {
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        kafka.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.ivanfranchin.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/v3/api-docs",
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

    // The API only publishes to the topic and never reads it back, so there is no state to reset
    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

}
