package em.embedded.kafkarest;

import io.confluent.kafkarest.KafkaRestApplication;
import io.confluent.kafkarest.KafkaRestConfig;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Server;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.kafka.KafkaContainer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String KAFKA_VERSION = "4.3.1";

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


    private Server server;

    private Admin admin;

    private Map<ConfigResource, Map<String, String>> brokerConfigs;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {

        kafka.start();
        admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
        try {
            brokerConfigs = dynamicBrokerConfigs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Properties props = new Properties();
        props.setProperty("listeners", "http://localhost:0");
        props.setProperty("bootstrap.servers", kafka.getBootstrapServers());
        props.setProperty("jetty.legacy.uri.compliance", "true");

        try {
            server = new KafkaRestApplication(new KafkaRestConfig(props)).createServer();
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
    }

    @Override
    public boolean isSutRunning() {
        return server != null && server.isRunning();
    }

    @Override
    public void stopSut() {
        if (server != null) {
            try {
                server.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            server = null;
        }
        if (admin != null) {
            admin.close();
            admin = null;
        }
        kafka.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "io.confluent.kafkarest.";
    }

    // all state lives in the broker: topics (with their records) and dynamic broker configs
    @Override
    public void resetStateOfSUT() {
        if (admin == null) {
            return;
        }
        try {
            Set<String> topics = admin.listTopics().names().get();
            if (!topics.isEmpty()) {
                admin.deleteTopics(topics).all().get();
                long deadline = System.currentTimeMillis() + 10_000;
                while (!admin.listTopics().names().get().isEmpty() && System.currentTimeMillis() < deadline) {
                    Thread.sleep(20);
                }
            }

            Map<ConfigResource, Map<String, String>> current = dynamicBrokerConfigs();
            Map<ConfigResource, Collection<AlterConfigOp>> ops = new HashMap<>();
            current.forEach((resource, now) -> {
                Map<String, String> initial = brokerConfigs.get(resource);
                List<AlterConfigOp> list = new ArrayList<>();
                now.keySet().stream().filter(k -> !initial.containsKey(k)).forEach(k ->
                        list.add(new AlterConfigOp(new ConfigEntry(k, null), AlterConfigOp.OpType.DELETE)));
                initial.forEach((k, v) -> {
                    if (!Objects.equals(now.get(k), v)) {
                        list.add(new AlterConfigOp(new ConfigEntry(k, v), AlterConfigOp.OpType.SET));
                    }
                });
                if (!list.isEmpty()) {
                    ops.put(resource, list);
                }
            });
            if (!ops.isEmpty()) {
                admin.incrementalAlterConfigs(ops).all().get();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset the Kafka broker", e);
        }
    }

    // the broker starts with some dynamic configs of its own (eg min.insync.replicas), so reset restores them
    private Map<ConfigResource, Map<String, String>> dynamicBrokerConfigs() throws Exception {
        List<ConfigResource> brokers = new ArrayList<>();
        brokers.add(new ConfigResource(ConfigResource.Type.BROKER, ""));
        for (Node node : admin.describeCluster().nodes().get()) {
            brokers.add(new ConfigResource(ConfigResource.Type.BROKER, node.idString()));
        }
        Map<ConfigResource, Map<String, String>> result = new HashMap<>();
        admin.describeConfigs(brokers).all().get().forEach((resource, config) -> {
            ConfigEntry.ConfigSource own = resource.name().isEmpty()
                    ? ConfigEntry.ConfigSource.DYNAMIC_DEFAULT_BROKER_CONFIG
                    : ConfigEntry.ConfigSource.DYNAMIC_BROKER_CONFIG;
            result.put(resource, config.entries().stream()
                    .filter(e -> e.source() == own && !e.isSensitive())
                    .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value)));
        });
        return result;
    }

    // EvoMaster cannot parse the templated server URL, and would drop the /v3 base path
    @Override
    public ProblemInfo getProblemInfo() {
        String schema = new Scanner(getClass().getResourceAsStream("/openapi.yaml"), "UTF-8").useDelimiter("\\A").next()
                .replace("\"{protocol}://{address}:{port}/v3\"", "\"/v3\"");
        return new RestProblem(null, null, schema);
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
