package em.embedded.redis.sample;

import id.my.hendisantika.springbootredissample.SpringBootRedisSampleApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

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

        redis.start();

        ctx = SpringApplication.run(SpringBootRedisSampleApplication.class, new String[]{
                "--server.port=0",
                "--spring.data.redis.host=" + redis.getHost(),
                "--spring.data.redis.port=" + redis.getMappedPort(REDIS_PORT),
                "--spring.data.redis.password=" + REDIS_PASSWORD,
                "--spring.autoconfigure.exclude=" + SECURITY_EXCLUDE,
                "--app.numberOfRatings=" + NUMBER_OF_RATINGS,
                "--app.ratingStars=" + RATING_STARS
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

        redis.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "id.my.hendisantika.springbootredissample.";
    }

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
