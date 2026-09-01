package em.external.digitalbanking;


import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.UnifiedJedis;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

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

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/digitalbanking/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/digitalbanking-sut.jar";
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

    private UnifiedJedis redisClient;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/digitalbanking-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
        Spring logs "Started ... in" before the CommandLineRunners, and the SUT is unusable until
        the RediSearch indexes exist. This is the last line that runner writes.
     */
    @Override
    public String getLogMessageOfInitializedServer() {
        return "rebuilding index on Disp";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        redis.start();
        kafka.start();
        cassandra.start();
        initCassandraSchema();
        redisClient = new JedisPooled("redis://:" + REDIS_PASSWORD + "@"
                + redis.getHost() + ":" + redis.getMappedPort(REDIS_PORT));
    }

    private void initCassandraSchema() {
        try {
            cassandra.execInContainer("cqlsh", "-e", INIT_CQL);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create the Cassandra schema", e);
        }
    }

    @Override
    public void postStart() {
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
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

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/v3/api-docs",
                ENDPOINTS_TO_SKIP
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

}
