package em.embedded.it.pagopa.pn.mandate;

import com.github.dockerjava.api.model.Ulimit;
import it.pagopa.pn.mandate.MandateApplication;
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
import org.testcontainers.utility.MountableFile;
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
import software.amazon.awssdk.services.dynamodb.model.StreamSpecification;
import software.amazon.awssdk.services.dynamodb.model.StreamViewType;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Embedded EvoMaster controller for pn-mandate. */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String LOCALSTACK_IMAGE = "localstack/localstack:4.4";
    private static final int LOCALSTACK_PORT = 4566;
    private static final String AWS_REGION = "us-east-1";

    private static final String MANDATE_TABLE = "Mandate";
    private static final String MANDATE_HISTORY_TABLE = "MandateHistory";
    private static final String FIFO_QUEUE = "local-mandate-inputs.fifo";
    private static final String OUTBOUND_QUEUE = "toDeliveryQueueNameTest";
    private static final String CSCA_BUCKET = "pn-runtime-environment-variables-eu-south-1-830192246553";
    private static final String CSCA_KEY = "pn-mandate/csca-masterlist/new_IT_MasterListCSCA.zip";
    private static final String SCHEMA_FILE = "pn-mandate.yaml";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer<?> localstack = new GenericContainer<>(LOCALSTACK_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withEnv("SERVICES", "dynamodb,sqs,ssm,s3")
            .withEnv("AWS_ACCESS_KEY_ID", "test")
            .withEnv("AWS_SECRET_ACCESS_KEY", "test")
            .withExposedPorts(LOCALSTACK_PORT)
            .withStartupTimeout(Duration.ofMinutes(3));

    private static String schemaLocation;

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
        createQueuesSsmAndS3();

        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");

        ctx = SpringApplication.run(MandateApplication.class, new String[]{
                "--server.port=0",
                "--spring.config.additional-location=classpath:/benchmark/application.properties",
                "--logging.config=classpath:logback-base.xml",
                "--pn.middleware.init.aws=true",
                "--aws.profile-name=",
                "--spring.cloud.aws.credentials.profile-name=",
                "--spring.cloud.aws.credentials.profile.name=",
                "--spring.cloud.aws.credentials.access-key=test",
                "--spring.cloud.aws.credentials.secret-key=test",
                "--aws.region-code=" + AWS_REGION,
                "--spring.cloud.aws.region.static=" + AWS_REGION,
                "--aws.endpoint-url=" + awsEndpoint,
                "--spring.cloud.aws.endpoint=" + awsEndpoint,
                "--spring.cloud.aws.sqs.endpoint=" + awsEndpoint
        });

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
        return "it.pagopa.pn.mandate.,it.pagopa.pn.ciechecker.";
    }

    @Override
    public void resetStateOfSUT() {
        clearTable(MANDATE_TABLE, "pk", "sk");
        clearTable(MANDATE_HISTORY_TABLE, "pk", "sk");
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(schemaLocation(), null);
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

    private static synchronized String schemaLocation() {
        if (schemaLocation == null) {
            try {
                Path dir = Files.createTempDirectory("pn-mandate-openapi");
                dir.toFile().deleteOnExit();
                try (InputStream in = EmbeddedEvoMasterController.class.getResourceAsStream("/" + SCHEMA_FILE)) {
                    if (in == null) {
                        throw new IllegalStateException("Missing OpenAPI resource: " + SCHEMA_FILE);
                    }
                    Files.copy(in, dir.resolve(SCHEMA_FILE));
                }
                dir.resolve(SCHEMA_FILE).toFile().deleteOnExit();
                schemaLocation = dir.resolve(SCHEMA_FILE).toUri().toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return schemaLocation;
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

    private void createTables() {
        dynamo.createTable(CreateTableRequest.builder()
                .tableName(MANDATE_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        attribute("pk", ScalarAttributeType.S),
                        attribute("sk", ScalarAttributeType.S),
                        attribute("s_delegate", ScalarAttributeType.S),
                        attribute("i_state", ScalarAttributeType.N))
                .keySchema(keyElement("pk", KeyType.HASH), keyElement("sk", KeyType.RANGE))
                .streamSpecification(StreamSpecification.builder()
                        .streamEnabled(true)
                        .streamViewType(StreamViewType.NEW_AND_OLD_IMAGES)
                        .build())
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName("delegate-state-gsi")
                        .keySchema(keyElement("s_delegate", KeyType.HASH), keyElement("i_state", KeyType.RANGE))
                        .projection(Projection.builder().projectionType(ProjectionType.ALL).build())
                        .build())
                .build());

        dynamo.createTable(CreateTableRequest.builder()
                .tableName(MANDATE_HISTORY_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        attribute("pk", ScalarAttributeType.S),
                        attribute("sk", ScalarAttributeType.S))
                .keySchema(keyElement("pk", KeyType.HASH), keyElement("sk", KeyType.RANGE))
                .build());
    }

    private void createQueuesSsmAndS3() {
        try {
            execChecked("awslocal", "sqs", "create-queue",
                    "--attributes", "{\"DelaySeconds\":\"2\",\"FifoQueue\":\"true\",\"ContentBasedDeduplication\":\"true\"}",
                    "--queue-name", FIFO_QUEUE);
            execChecked("awslocal", "sqs", "create-queue",
                    "--queue-name", OUTBOUND_QUEUE);
            execChecked("awslocal", "ssm", "put-parameter",
                    "--name", "AARQrUrlConfigs",
                    "--type", "String",
                    "--overwrite",
                    "--value", "{\"1.0.0\": {\"directAccessUrlTemplatePhysical\": \"https://cittadini.dev.notifichedigitali.it/\",\"directAccessUrlTemplateLegal\": \"https://imprese.dev.notifichedigitali.it/\",\"quickAccessUrlAarDetailSuffix\": \"?aar\"}}");

            try (InputStream in = getClass().getResourceAsStream("/new_IT_MasterListCSCA.zip")) {
                if (in == null) {
                    throw new IllegalStateException("Missing CSCA masterlist zip resource");
                }
                Path tmpZip = Files.createTempFile("new_IT_MasterListCSCA", ".zip");
                try {
                    Files.copy(in, tmpZip, StandardCopyOption.REPLACE_EXISTING);
                    localstack.copyFileToContainer(MountableFile.forHostPath(tmpZip), "/tmp/new_IT_MasterListCSCA.zip");
                } finally {
                    Files.deleteIfExists(tmpZip);
                }
            }
            Container.ExecResult mb = localstack.execInContainer("awslocal", "s3", "mb", "s3://" + CSCA_BUCKET);
            if (mb.getExitCode() != 0 && !mb.getStderr().contains("BucketAlreadyOwnedByYou")) {
                throw new IllegalStateException("S3 bucket creation failed: " + mb.getStdout() + mb.getStderr());
            }
            execChecked("awslocal", "s3", "cp",
                    "/tmp/new_IT_MasterListCSCA.zip",
                    "s3://" + CSCA_BUCKET + "/" + CSCA_KEY);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private void execChecked(String... command) throws IOException, InterruptedException {
        Container.ExecResult result = localstack.execInContainer(command);
        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Command " + String.join(" ", command) + " failed: " + result.getStdout() + result.getStderr());
        }
    }

    private void clearTable(String table, String... keys) {
        Map<String, AttributeValue> startKey = null;
        do {
            ScanRequest.Builder scan = ScanRequest.builder().tableName(table);
            if (startKey != null && !startKey.isEmpty()) {
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
        } while (startKey != null && !startKey.isEmpty());
    }

    private static AttributeDefinition attribute(String name, ScalarAttributeType type) {
        return AttributeDefinition.builder().attributeName(name).attributeType(type).build();
    }

    private static KeySchemaElement keyElement(String name, KeyType type) {
        return KeySchemaElement.builder().attributeName(name).keyType(type).build();
    }
}
