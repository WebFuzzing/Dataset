package em.embedded.kafkaproducer;

import com.ivanfranchin.producerapi.ProducerApiApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String KAFKA_VERSION = "3.8.1";

    private static final int KAFKA_PORT = 9092;

    private static final KafkaContainer kafka = new KafkaContainer("apache/kafka:" + KAFKA_VERSION);


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

        kafka.start();

        // Eureka and Zipkin only serve the demo's infrastructure, not the API
        ctx = SpringApplication.run(ProducerApiApplication.class, new String[]{
                "--server.port=0",
                "--KAFKA_HOST=" + kafka.getHost(),
                "--KAFKA_PORT=" + kafka.getMappedPort(KAFKA_PORT),
                "--eureka.client.enabled=false",
                "--management.tracing.export.zipkin.enabled=false"
        });

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

        kafka.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.ivanfranchin.";
    }

    // The API only publishes to the topic and never reads it back, so there is no state to reset
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
