package em.external.com.datastax.mgmtapi;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    // upstream image of the same project: Cassandra with the management agent already on its JVM
    private static final String CASSANDRA_IMAGE = "k8ssandra/cass-management-api:4.1.12-v0.1.126";

    private static final String CASSANDRA_DATACENTER = "datacenter1";

    // the agent serves the management CALLs only on this unix socket
    private static final String AGENT_SOCKET = "/tmp/mgmtapi.sock";

    private static final int BRIDGE_PORT = 9999;

    // read by the SUT (MODIFIED UnixSocketCQLAccess), the CQL session then goes over the bridge
    private static final String TCP_BRIDGE_PROPERTY = "mgmtapi.cql.tcp";

    private static final Set<String> SYSTEM_KEYSPACES = new HashSet<>(Arrays.asList(
            "system", "system_auth", "system_distributed", "system_schema",
            "system_traces", "system_views", "system_virtual_schema"));

    // the bridge (TCP to the agent socket) runs inside the container, as in the BB docker-compose
    private static final GenericContainer<?> cassandra = new GenericContainer<>(CASSANDRA_IMAGE)
            .withEnv("HEAP_NEWSIZE", "128M")
            .withEnv("MAX_HEAP_SIZE", "512M")
            .withEnv("JVM_EXTRA_OPTS", "-Ddb.unix_socket_file=" + AGENT_SOCKET)
            .withCopyFileToContainer(MountableFile.forClasspathResource("mgmtapi-bridge.py"), "/mgmtapi-bridge.py")
            .withCommand("bash", "-c",
                    "python3 /mgmtapi-bridge.py " + AGENT_SOCKET + " " + BRIDGE_PORT + " & exec cassandra -f")
            .withExposedPorts(BRIDGE_PORT)
            .waitingFor(Wait.forLogMessage(".*Startup complete.*", 1))
            .withStartupTimeout(Duration.ofMinutes(5));


    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/management-cassandra/management-api-server/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/management-cassandra-sut.jar";
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

    private CqlSession cql;

    private Map<String, Map<String, String>> systemReplication;

    private Set<String> initialRoles;

    private Set<String> systemTables;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../management-api-server/target/management-cassandra-sut.jar",
                DEFAULT_SUT_PORT, 120, "java");
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

    /*
        -K: no keep-alive, Cassandra runs in its own container and is not managed by the API.
        Over the bridge the socket file is only a session cache key, it does not need to exist.
     */
    @Override
    public String[] getInputParameters() {
        return new String[]{
                "-K", "true",
                "-H", "tcp://localhost:" + sutPort,
                "-S", "mgmtapi.sock"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{
                "-D" + TCP_BRIDGE_PROPERTY + "=" + cassandra.getHost() + ":" + cassandra.getMappedPort(BRIDGE_PORT)
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
        return "Started service on";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        cassandra.start();
        cql = CqlSession.builder()
                // through the bridge too: an internal client may drop superuser roles
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(BRIDGE_PORT)))
                .withLocalDatacenter(CASSANDRA_DATACENTER)
                // like the SUT: the agent socket silently drops the higher protocol versions the driver tries first
                .withConfigLoader(DriverConfigLoader.programmaticBuilder()
                        .withString(DefaultDriverOption.PROTOCOL_VERSION, "V4")
                        // as the SUT: dropping a keyspace snapshots it first, the 2 s default is too short
                        .withDuration(DefaultDriverOption.REQUEST_TIMEOUT, Duration.ofSeconds(30))
                        .build())
                .build();
        takeSnapshotOfState();
    }

    @Override
    public void postStart() {
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        if (cql != null) {
            cql.close();
            cql = null;
        }
        cassandra.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.datastax.mgmtapi.";
    }

    private void takeSnapshotOfState() {
        systemReplication = new HashMap<>();
        for (Row row : cql.execute("SELECT keyspace_name, replication FROM system_schema.keyspaces")) {
            if (SYSTEM_KEYSPACES.contains(row.getString(0))) {
                systemReplication.put(row.getString(0), row.getMap(1, String.class, String.class));
            }
        }
        systemTables = new HashSet<>();
        for (Row row : cql.execute("SELECT keyspace_name, table_name FROM system_schema.tables")) {
            if (SYSTEM_KEYSPACES.contains(row.getString(0))) {
                systemTables.add(row.getString(0) + "." + row.getString(1));
            }
        }
        // Cassandra creates its default role a few seconds after startup
        initialRoles = new HashSet<>(Collections.singletonList("cassandra"));
        for (Row row : cql.execute("SELECT role FROM system_auth.roles")) {
            initialRoles.add(row.getString(0));
        }
    }

    /*
        Restores keyspaces, system keyspace replication and tables, roles and snapshots. Not restored:
        logging level and compaction throughput, which no endpoint reads back.
     */
    @Override
    public void resetStateOfSUT() {
        if (cql == null) {
            return;
        }
        List<String> keyspaces = new ArrayList<>();
        for (Row row : cql.execute("SELECT keyspace_name, replication FROM system_schema.keyspaces")) {
            String keyspace = row.getString(0);
            keyspaces.add(keyspace);
            String name = CqlIdentifier.fromInternal(keyspace).asCql(true);
            Map<String, String> initial = systemReplication.get(keyspace);
            if (!SYSTEM_KEYSPACES.contains(keyspace)) {
                cql.execute("DROP KEYSPACE IF EXISTS " + name);
            } else if (initial != null && !initial.equals(row.getMap(1, String.class, String.class))) {
                cql.execute("ALTER KEYSPACE " + name + " WITH replication = " + asCqlMap(initial));
            }
        }
        // the API can also create tables in the replicated system keyspaces
        for (Row row : cql.execute("SELECT keyspace_name, table_name FROM system_schema.tables")) {
            if (SYSTEM_KEYSPACES.contains(row.getString(0))
                    && !systemTables.contains(row.getString(0) + "." + row.getString(1))) {
                cql.execute("DROP TABLE IF EXISTS " + CqlIdentifier.fromInternal(row.getString(0)).asCql(true)
                        + "." + CqlIdentifier.fromInternal(row.getString(1)).asCql(true));
            }
        }
        for (Row row : cql.execute("SELECT role FROM system_auth.roles")) {
            if (!initialRoles.contains(row.getString(0))) {
                // CQL DROP ROLE needs a logged-in user, the socket's internal client has none; same RPC as the SUT
                cql.execute(SimpleStatement.newInstance("CALL NodeOps.dropRole(?)", row.getString(0)));
            }
        }
        // drops above take auto snapshots too; without explicit names the agent skips dropped keyspaces
        cql.execute(SimpleStatement.newInstance("CALL NodeOps.clearSnapshots(?, ?)", null, keyspaces));
    }

    private static String asCqlMap(Map<String, String> map) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        map.forEach((k, v) -> joiner.add("'" + k + "': '" + v + "'"));
        return joiner.toString();
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/openapi.json",
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
