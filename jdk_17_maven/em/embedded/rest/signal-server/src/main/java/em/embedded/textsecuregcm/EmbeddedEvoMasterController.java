package em.embedded.textsecuregcm;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.whispersystems.textsecuregcm.WhisperServerService;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;

public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final int DYNAMODB_PORT = 8000;

    private static final String DYNAMODB_VERSION = "1.25.0";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer dynamoDBContainer = new GenericContainer("amazon/dynamodb-local:" + DYNAMODB_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(DYNAMODB_PORT);

    private static final int REDIS_PORT = 6379;

    private static final String REDIS_VERSION = "7.2.3";

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:" + REDIS_VERSION);

    private static JedisPool jedisPool;

    private static final GenericContainer redisContainer = new GenericContainer(REDIS_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(REDIS_PORT)
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withEnv("REDIS_NODES", "redis-cluster-01")
            .withCopyFileToContainer(MountableFile.forHostPath("src/main/resources/redis.conf"), "/usr/local/etc/redis/redis.conf")
            .withCommand("redis-server /usr/local/etc/redis/redis.conf");

    public static void main(String[] args) {

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    private WhisperServerService application;

    @Override
    public boolean isSutRunning() {
        if (application == null) {
            return false;
        }

        return application.getJettyServer().isRunning();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.whispersystems.textsecuregcm.";
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
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
        System.setProperty("aws.region", "us-west-2");

        redisContainer.start();
        jedisPool = new JedisPool(redisContainer.getHost(), redisContainer.getMappedPort(REDIS_PORT));

        dynamoDBContainer.start();

        application = new WhisperServerService();

        String redisURL = "redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(REDIS_PORT) + "/";

        //Dirty hack for DW...
        System.setProperty("dw.server.applicationConnectors[0].port", "0");

        System.setProperty("dw.cacheCluster.configurationUri", redisURL);
        System.setProperty("dw.clientPresenceCluster.configurationUri", redisURL);
        System.setProperty("dw.pubsub.uri", redisURL);
        System.setProperty("dw.pushSchedulerCluster.configurationUri", redisURL);
        System.setProperty("dw.rateLimitersCluster.configurationUri", redisURL);
        System.setProperty("dw.messageCache.cluster.configurationUri", redisURL);
        System.setProperty("dw.metricsCluster.configurationUri", redisURL);

        System.setProperty("secrets.bundle.filename", "src/main/resources/secrets.yml");

        try {
            application.run("server", "src/main/resources/em-sample.yml");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {

        }

        while(!application.getJettyServer().isStarted()) {
            try {
                Thread.sleep(3_000);
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
        dynamoDBContainer.stop();
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
