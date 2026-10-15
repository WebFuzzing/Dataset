package em.external.kafkarest;


import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.testcontainers.kafka.KafkaContainer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String KAFKA_VERSION = "4.3.1";

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
        String jarLocation = "cs/rest/kafka-rest/kafka-rest/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/kafka-rest-sut.jar";
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

    private Path configFile;

    private Admin admin;

    private Map<ConfigResource, Map<String, String>> brokerConfigs;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/kafka-rest-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
        return new String[]{configFile.toAbsolutePath().toString()};
    }

    // the jar has no log4j2 config, and the default ERROR level hides the startup line
    @Override
    public String[] getJVMParameters() {
        return new String[]{"-Dorg.apache.logging.log4j.level=INFO"};
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
        return "Server started, listening for requests";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        kafka.start();
        try {
            configFile = Files.createTempFile("kafka-rest", ".properties");
            Files.write(configFile, List.of(
                    "listeners=http://localhost:" + sutPort,
                    "bootstrap.servers=" + kafka.getBootstrapServers(),
                    "jetty.legacy.uri.compliance=true"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        admin = Admin.create(Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()));
        try {
            brokerConfigs = dynamicBrokerConfigs();
        } catch (Exception e) {
            throw new RuntimeException(e);
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
        if (admin != null) {
            admin.close();
            admin = null;
        }
        kafka.stop();
        if (configFile != null) {
            configFile.toFile().delete();
            configFile = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "io.confluent.kafkarest.";
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

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

}
