package em.embedded.reciter;

import com.github.dockerjava.api.model.Ulimit;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import reciter.Application;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String LOCALSTACK_IMAGE = "localstack/localstack:4.4";

    private static final int LOCALSTACK_PORT = 4566;

    private static final String AWS_REGION = "us-east-1";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer<?> localstack = new GenericContainer<>(LOCALSTACK_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withEnv("SERVICES", "dynamodb")
            .withEnv("DEFAULT_REGION", AWS_REGION)
            .withExposedPorts(LOCALSTACK_PORT)
            .withStartupTimeout(Duration.ofMinutes(3));

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
    private DynamoDbClient dynamo;
    private final Map<String, List<String>> tableKeys = new LinkedHashMap<>();

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {
        tableKeys.clear();
        localstack.start();
        int mappedPort = localstack.getMappedPort(LOCALSTACK_PORT);
        String awsEndpoint = "http://" + localstack.getHost() + ":" + mappedPort;
        dynamo = DynamoDbClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        awaitDynamoDb();

        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");

        String logFile = Path.of(System.getProperty("java.io.tmpdir"), "reciter-embedded-sut.log").toString();

        ctx = SpringApplication.run(Application.class, new String[]{
                "--server.port=0",
                "--logging.file.name=" + logFile,
                "--springdoc.api-docs.version=OPENAPI_3_0",
                "--spring.security.enabled=false",
                "--aws.dynamoDb.local=true",
                "--aws.dynamoDb.local.port=" + mappedPort,
                "--aws.dynamoDb.local.region=" + AWS_REGION,
                "--aws.dynamoDb.local.accesskey=test",
                "--aws.dynamoDb.local.secretkey=test",
                "--aws.dynamodb.settings.table.create=true",
                "--aws.s3.use=false",
                "--aws.s3.use.cached.identityAll=false"
        });

        cacheTableKeys();

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return (Integer) ((Map<?, ?>) ctx.getEnvironment()
                .getPropertySources().get("server.ports").getSource())
                .get("local.server.port");
    }

    @Override
    public boolean isSutRunning() {
        return ctx != null && ctx.isRunning();
    }

    @Override
    public void stopSut() {
        tableKeys.clear();
        if (ctx != null) {
            ctx.stop();
            ctx.close();
            ctx = null;
        }
        if (dynamo != null) {
            dynamo.close();
            dynamo = null;
        }
        localstack.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "reciter.";
    }

    @Override
    public void resetStateOfSUT() {
        if (tableKeys.isEmpty()) {
            cacheTableKeys();
        }
        for (Map.Entry<String, List<String>> entry : tableKeys.entrySet()) {
            clearTable(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/reciter/v3/api-docs",
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

    @Override
    public Object getDynamoDbConnection() {
        return dynamo;
    }

    private void awaitDynamoDb() {
        long deadline = System.currentTimeMillis() + 120_000;
        while (true) {
            try {
                dynamo.listTables();
                return;
            } catch (RuntimeException e) {
                if (System.currentTimeMillis() > deadline) {
                    throw e;
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw e;
                }
            }
        }
    }

    private void cacheTableKeys() {
        tableKeys.clear();
        for (String table : dynamo.listTables().tableNames()) {
            List<String> keys = new ArrayList<>();
            for (KeySchemaElement element : dynamo.describeTable(r -> r.tableName(table)).table().keySchema()) {
                keys.add(element.attributeName());
            }
            tableKeys.put(table, keys);
        }
    }

    private void clearTable(String table, List<String> keyNames) {
        Map<String, String> names = new HashMap<>();
        StringBuilder projection = new StringBuilder();
        for (int i = 0; i < keyNames.size(); i++) {
            String keyName = keyNames.get(i);
            String placeholder = "#k" + i;
            names.put(placeholder, keyName);
            if (i > 0) {
                projection.append(",");
            }
            projection.append(placeholder);
        }

        ScanRequest scan = ScanRequest.builder()
                .tableName(table)
                .projectionExpression(projection.toString())
                .expressionAttributeNames(names)
                .build();

        for (Map<String, AttributeValue> item : dynamo.scanPaginator(scan).items()) {
            Map<String, AttributeValue> key = new HashMap<>();
            for (String keyName : keyNames) {
                key.put(keyName, item.get(keyName));
            }
            dynamo.deleteItem(DeleteItemRequest.builder().tableName(table).key(key).build());
        }
    }
}
