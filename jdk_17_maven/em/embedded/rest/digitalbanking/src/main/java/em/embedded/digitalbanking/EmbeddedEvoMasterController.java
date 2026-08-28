package em.embedded.digitalbanking;

import com.jphaugla.DemoApplication;
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
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class used to start/stop the SUT. 
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    /*
        The SUT uses RediSearch (FT.CREATE), so it needs Redis Stack and not plain Redis.
     */
    private static final String REDIS_VERSION = "7.4.0-v1";

    private static final int REDIS_PORT = 6379;

    private static final String REDIS_PASSWORD = "jasonrocks";

    private static final String CASSANDRA_VERSION = "4.1";

    private static final int CASSANDRA_PORT = 9042;

    private static final String CASSANDRA_CLUSTER = "test";

    private static final String CASSANDRA_DATACENTER = "datacenter1";

    private static final String KAFKA_VERSION = "3.8.1";

    private static final int KAFKA_PORT = 9092;

    /*
        The SUT connects to an existing keyspace, and the "transaction" table is the one
        scripts/trans.cql creates. Neither is created by the application itself.
     */
    private static final String INIT_CQL =
            "CREATE KEYSPACE IF NOT EXISTS banking WITH replication = " +
                    "{'class':'SimpleStrategy','replication_factor':1};" +
                    "CREATE TABLE IF NOT EXISTS banking.transaction (tranid text, accountno text," +
                    " amounttype text, merchant text, referencekeytype text, referencekeyvalue text," +
                    " originalamount text, amount text, trancd text, description text, initialdate text," +
                    " settlementdate text, postingdate text, status text, disputeid text," +
                    " transactionreturn text, location text, transactiontags text, primary key (tranid));";

    /*
        /generateData loops on its request parameters with no upper bound. Fuzzed with a large
        value, it drives the SUT into a GC death-spiral it never recovers from.
     */
    private static final List<String> ENDPOINTS_TO_SKIP = Arrays.asList("/generateData");

    private static final GenericContainer redis = new GenericContainer("redis/redis-stack-server:" + REDIS_VERSION)
            .withEnv("REDIS_ARGS", "--requirepass " + REDIS_PASSWORD)
            .withExposedPorts(REDIS_PORT);

    private static final GenericContainer cassandra = new GenericContainer("cassandra:" + CASSANDRA_VERSION)
            .withEnv("CASSANDRA_CLUSTER_NAME", CASSANDRA_CLUSTER)
            .withEnv("CASSANDRA_DC", CASSANDRA_DATACENTER)
            .withEnv("CASSANDRA_ENDPOINT_SNITCH", "SimpleSnitch")
            .withEnv("HEAP_NEWSIZE", "128M")
            .withEnv("MAX_HEAP_SIZE", "512M")
            .withExposedPorts(CASSANDRA_PORT)
            .waitingFor(Wait.forLogMessage(".*Startup complete.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));

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

    private UnifiedJedis redisClient;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        redis.start();
        kafka.start();
        cassandra.start();
        initCassandraSchema();
        redisClient = new JedisPooled("redis://:" + REDIS_PASSWORD + "@"
                + redis.getHost() + ":" + redis.getMappedPort(REDIS_PORT));

        ctx = SpringApplication.run(DemoApplication.class, new String[]{
                "--server.port=0",
                "--REDIS_HOST=" + redis.getHost(),
                "--REDIS_PORT=" + redis.getMappedPort(REDIS_PORT),
                "--REDIS_PASSWORD=" + REDIS_PASSWORD,
                // the index creation runner reads these two instead of the spring.redis ones
                "--redis.host=" + redis.getHost(),
                "--redis.port=" + redis.getMappedPort(REDIS_PORT),
                "--KAFKA_HOST=" + kafka.getHost(),
                "--KAFKA_PORT=" + kafka.getMappedPort(KAFKA_PORT),
                "--SPRING_CASSANDRA_HOST=" + cassandra.getHost(),
                "--SPRING_CASSANDRA_PORT=" + cassandra.getMappedPort(CASSANDRA_PORT),
                "--SPRING_CASSANDRA_CLUSTER=" + CASSANDRA_CLUSTER,
                "--SPRING_CASSANDRA_DATACENTER=" + CASSANDRA_DATACENTER
        });

        return "http://localhost:" + getSutPort();
    }

    private void initCassandraSchema() {
        try {
            cassandra.execInContainer("cqlsh", "-e", INIT_CQL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create the Cassandra schema", e);
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

        if (redisClient != null) {
            redisClient.close();
            redisClient = null;
        }

        cassandra.stop();
        kafka.stop();
        redis.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.jphaugla.";
    }

    /*
        Redis is the only store that keeps state across tests. FLUSHALL would drop the
        RediSearch indexes, which the SUT creates only at startup.
     */
    @Override
    public void resetStateOfSUT() {
        if (redisClient == null) {
            return;
        }
        Set<String> keys = redisClient.keys("*");
        if (!keys.isEmpty()) {
            redisClient.del(keys.toArray(new String[0]));
        }
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
                ENDPOINTS_TO_SKIP
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }
}
