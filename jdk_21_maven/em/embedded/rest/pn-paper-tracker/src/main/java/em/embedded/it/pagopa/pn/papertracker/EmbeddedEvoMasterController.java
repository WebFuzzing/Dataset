package em.embedded.it.pagopa.pn.papertracker;

import it.pagopa.pn.papertracker.PaperTrackerApplication;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.Projection;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String LOCALSTACK_IMAGE = "localstack/localstack:4.4";

    private static final int LOCALSTACK_PORT = 4566;

    private static final String AWS_REGION = "us-east-1";

    private static final GenericContainer localstack = new GenericContainer(LOCALSTACK_IMAGE)
            .withEnv("SERVICES", "dynamodb,sqs")
            .withExposedPorts(LOCALSTACK_PORT)
            .withStartupTimeout(Duration.ofMinutes(3));

    // the producers look these up at startup (src/test/resources/testcontainers/init.sh)
    private static final List<String> QUEUES = Arrays.asList(
            "pn-ocr_outputs",
            "dl-sqs",
            "pn-external_channel_to_paper_tracker",
            "pn-external_channel_outputs",
            "pn-external_channel_to_paper_channel_dryrun",
            "pn-external_channel_to_paper_channel"
    );

    private static final String TRACKINGS_TABLE = "pn-PaperTrackings";
    private static final String ERRORS_TABLE = "pn-PaperTrackingsErrors";
    private static final String OUTPUTS_TABLE = "pn-PaperTrackerDryRunOutputs";

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

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {

        localstack.start();
        String awsEndpoint = "http://" + localstack.getHost() + ":" + localstack.getMappedPort(LOCALSTACK_PORT);
        dynamo = DynamoDbClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        awaitDynamoDb();
        createTables();
        createQueues();

        // read by the SDK's default credentials chain of the SUT's own clients
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");

        ctx = SpringApplication.run(PaperTrackerApplication.class, new String[]{
                "--server.port=0",
                // the SUT's own local config (config/application.properties), overridden below
                "--spring.config.additional-location=classpath:/pn-paper-tracker/application.properties",
                "--logging.config=classpath:logback-base.xml",
                "--logging.level.io.awspring.cloud=INFO",
                "--logging.level.software.amazon.awssdk=INFO",
                "--logging.level.software.amazon.awssdk.request=INFO",
                "--aws.profile-name=",
                "--spring.cloud.aws.credentials.profile-name=",
                "--aws.endpoint-url=" + awsEndpoint,
                "--spring.cloud.aws.endpoint=" + awsEndpoint,
                "--spring.cloud.aws.sqs.endpoint=" + awsEndpoint,
                "--pn.paper-tracker.topics.queue-ocr-inputs-url=" + awsEndpoint + "/000000000000/dl-sqs",
                // empty in the local config, so /init always failed; value from scripts/aws/cfn/application-dev.env
                "--pn.paper-tracker.enable-ocr-validation-for=1970-01-01;AR:DRY;890:DRY"
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
        if (dynamo != null) {
            dynamo.close();
            dynamo = null;
        }
        localstack.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "it.pagopa.pn.papertracker.";
    }

    @Override
    public void resetStateOfSUT() {
        clearTable(TRACKINGS_TABLE, "trackingId");
        clearTable(ERRORS_TABLE, "trackingId", "created");
        clearTable(OUTPUTS_TABLE, "trackingId", "created");
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(null, null, readResource("/pn-paper-tracker.json"));
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

    private static String readResource(String name) {
        try (InputStream in = EmbeddedEvoMasterController.class.getResourceAsStream(name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    // same schema as src/test/resources/testcontainers/init.sh
    private void createTables() {
        for (String table : Arrays.asList(ERRORS_TABLE, OUTPUTS_TABLE)) {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName(table)
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .attributeDefinitions(stringAttribute("trackingId"), stringAttribute("created"))
                    .keySchema(keyElement("trackingId", KeyType.HASH), keyElement("created", KeyType.RANGE))
                    .build());
        }

        dynamo.createTable(CreateTableRequest.builder()
                .tableName(TRACKINGS_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        stringAttribute("trackingId"),
                        stringAttribute("ocrRequestId"),
                        stringAttribute("attemptId"),
                        stringAttribute("pcRetry"))
                .keySchema(keyElement("trackingId", KeyType.HASH))
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("ocrRequestId-index")
                                .keySchema(keyElement("ocrRequestId", KeyType.HASH))
                                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                .build(),
                        GlobalSecondaryIndex.builder()
                                .indexName("attemptId-pcRetry-index")
                                .keySchema(keyElement("attemptId", KeyType.HASH), keyElement("pcRetry", KeyType.RANGE))
                                .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                                .build())
                .build());
    }

    private void createQueues() {
        for (String queue : QUEUES) {
            try {
                Container.ExecResult result = localstack.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", queue);
                if (result.getExitCode() != 0) {
                    throw new IllegalStateException("Cannot create queue " + queue + ": " + result.getStderr());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    private void clearTable(String table, String... keys) {
        Map<String, AttributeValue> startKey = null;
        do {
            ScanRequest.Builder scan = ScanRequest.builder().tableName(table);
            if (startKey != null) {
                scan.exclusiveStartKey(startKey);
            }
            var page = dynamo.scan(scan.build());
            for (Map<String, AttributeValue> item : page.items()) {
                Map<String, AttributeValue> key = new HashMap<>();
                for (String k : keys) {
                    key.put(k, item.get(k));
                }
                dynamo.deleteItem(DeleteItemRequest.builder().tableName(table).key(key).build());
            }
            startKey = page.hasLastEvaluatedKey() ? page.lastEvaluatedKey() : null;
        } while (startKey != null);
    }

    private static AttributeDefinition stringAttribute(String name) {
        return AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build();
    }

    private static KeySchemaElement keyElement(String name, KeyType type) {
        return KeySchemaElement.builder().attributeName(name).keyType(type).build();
    }
}
