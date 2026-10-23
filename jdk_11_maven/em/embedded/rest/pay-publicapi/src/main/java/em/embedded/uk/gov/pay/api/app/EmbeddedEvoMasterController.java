package em.embedded.uk.gov.pay.api.app;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;
import uk.gov.pay.api.app.PublicApi;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;
import java.util.List;

public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final int REDIS_PORT = 6379;

    private static final String REDIS_VERSION = "7.2.3";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer redisContainer = new GenericContainer("redis:" + REDIS_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(REDIS_PORT);

    private static String REDIS_URL = "";

    private static JedisPool jedisPool;

    public static void main(String[] args) {
        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private PublicApi application;

    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public boolean isSutRunning() {
        if (application == null) {
            return false;
        }

        return application.getJettyServer().isRunning();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "uk.gov.pay.api.";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        AuthenticationDto dto = AuthUtils.getForAuthorizationHeader("foo", "Bearer asdfghdasdjlguuolnga94upq3nrd2642sq7uel0oo");
        dto.setRequireMockHandling(true);
        return Arrays.asList(dto);
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + application.getJettyPort() + "/assets/swagger.json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public String startSut() {
        redisContainer.start();

        REDIS_URL = redisContainer.getHost() + redisContainer.getMappedPort(REDIS_PORT);

        jedisPool = new JedisPool(redisContainer.getHost(), redisContainer.getMappedPort(REDIS_PORT));

        application = new PublicApi();

        //Dirty hack for DW...
        System.setProperty("dw.server.applicationConnectors[0].port", "0");
        System.setProperty("dw.server.adminConnectors[0].port", "0");
        System.setProperty("dw.redis.endpoint", REDIS_URL);

        /*
        Note: When running using IntelliJ, make sure the working directory is set to the
        driver module.
         */
        try {
            application.run("server", "src/main/resources/em_config.yaml");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {

        }
        while (!application.getJettyServer().isStarted()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }

        return "http://localhost:" + application.getJettyPort();
    }

    @Override
    public void stopSut() {
        if (application != null) {
            try {
                application.getJettyServer().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        redisContainer.stop();
    }

    @Override
    public void resetStateOfSUT() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushAll();
        }
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }
}
