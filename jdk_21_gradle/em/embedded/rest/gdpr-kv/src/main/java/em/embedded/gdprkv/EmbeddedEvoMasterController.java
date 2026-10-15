package em.embedded.gdprkv;

import com.example.gdprkv.GdprKvApplication;
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
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.net.URI;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

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

    private static final int LOCALSTACK_PORT = 4566;

    private static final String LOCALSTACK_VERSION = "3.6";

    private static final String AWS_REGION = "us-west-2";

    private static final GenericContainer localstackContainer = new GenericContainer("localstack/localstack:" + LOCALSTACK_VERSION)
            .withEnv("SERVICES", "dynamodb")
            .withEnv("DEFAULT_REGION", AWS_REGION)
            .withExposedPorts(LOCALSTACK_PORT)
            .withStartupTimeout(Duration.ofMinutes(3));

    // the SUT never creates its tables: without them every endpoint fails with a 500
    private static final String SUBJECTS_TABLE = "subjects";
    private static final String POLICIES_TABLE = "policies";
    private static final String RECORDS_TABLE = "records";
    private static final String AUDIT_EVENTS_TABLE = "audit_events";
    private static final String PURGE_INDEX = "records_by_purge_due";

    private static final String SEED_PURPOSE = "DEMO_PURPOSE";
    private static final String SEED_SUBJECT_ID = "demo_subject_001";
    private static final String SEED_RECORD_KEY = "pref:email";
    private static final long SEED_TIMESTAMP = 1700000000000L;

    private String awsEndpoint;

    private DynamoDbClient dynamo;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        localstackContainer.start();
        awsEndpoint = "http://" + localstackContainer.getHost() + ":" + localstackContainer.getMappedPort(LOCALSTACK_PORT);
        dynamo = DynamoDbClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        awaitDynamoDb();
        createTables();
        seedData();

        ctx = SpringApplication.run(GdprKvApplication.class,
                new String[]{"--server.port=0",
                        "--server.aws.endpoint=" + awsEndpoint,
                        "--server.aws.region=" + AWS_REGION,
                        "--server.aws.use-localstack=true"
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
        ctx.stop();
        ctx.close();

        if (dynamo != null) {
            dynamo.close();
            dynamo = null;
        }
        localstackContainer.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.gdprkv.";
    }

    @Override
    public void resetStateOfSUT() {
        clearTable(AUDIT_EVENTS_TABLE, "subject_id", "ts_ulid");
        clearTable(RECORDS_TABLE, "subject_id", "record_key");
        clearTable(SUBJECTS_TABLE, "subject_id");
        clearTable(POLICIES_TABLE, "purpose");
        seedData();
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
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
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
        createTable(CreateTableRequest.builder()
                .tableName(SUBJECTS_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(stringAttribute("subject_id"))
                .keySchema(keyElement("subject_id", KeyType.HASH))
                .build());

        createTable(CreateTableRequest.builder()
                .tableName(POLICIES_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(stringAttribute("purpose"))
                .keySchema(keyElement("purpose", KeyType.HASH))
                .build());

        createTable(CreateTableRequest.builder()
                .tableName(RECORDS_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        stringAttribute("subject_id"),
                        stringAttribute("record_key"),
                        stringAttribute("purge_bucket"),
                        AttributeDefinition.builder().attributeName("purge_due_at").attributeType(ScalarAttributeType.N).build())
                .keySchema(
                        keyElement("subject_id", KeyType.HASH),
                        keyElement("record_key", KeyType.RANGE))
                .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                        .indexName(PURGE_INDEX)
                        .keySchema(
                                keyElement("purge_bucket", KeyType.HASH),
                                keyElement("purge_due_at", KeyType.RANGE))
                        .projection(Projection.builder().projectionType(ProjectionType.KEYS_ONLY).build())
                        .build())
                .build());

        createTable(CreateTableRequest.builder()
                .tableName(AUDIT_EVENTS_TABLE)
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(
                        stringAttribute("subject_id"),
                        stringAttribute("ts_ulid"))
                .keySchema(
                        keyElement("subject_id", KeyType.HASH),
                        keyElement("ts_ulid", KeyType.RANGE))
                .build());
    }

    private void createTable(CreateTableRequest request) {
        try {
            dynamo.createTable(request);
        } catch (ResourceInUseException e) {
        }
        dynamo.waiter().waitUntilTableExists(r -> r.tableName(request.tableName()));
    }

    private static AttributeDefinition stringAttribute(String name) {
        return AttributeDefinition.builder().attributeName(name).attributeType(ScalarAttributeType.S).build();
    }

    private static KeySchemaElement keyElement(String name, KeyType type) {
        return KeySchemaElement.builder().attributeName(name).keyType(type).build();
    }

    private void seedData() {
        Map<String, AttributeValue> policy = new HashMap<>();
        policy.put("purpose", AttributeValue.fromS(SEED_PURPOSE));
        policy.put("retention_days", AttributeValue.fromN("1"));
        policy.put("description", AttributeValue.fromS("Demo retention policy"));
        policy.put("last_updated_at", AttributeValue.fromN(String.valueOf(SEED_TIMESTAMP)));
        dynamo.putItem(PutItemRequest.builder().tableName(POLICIES_TABLE).item(policy).build());

        Map<String, AttributeValue> subject = new HashMap<>();
        subject.put("subject_id", AttributeValue.fromS(SEED_SUBJECT_ID));
        subject.put("created_at", AttributeValue.fromN(String.valueOf(SEED_TIMESTAMP)));
        subject.put("residency", AttributeValue.fromS("US"));
        subject.put("request_id", AttributeValue.fromS("seed"));
        dynamo.putItem(PutItemRequest.builder().tableName(SUBJECTS_TABLE).item(subject).build());

        Map<String, AttributeValue> record = new HashMap<>();
        record.put("subject_id", AttributeValue.fromS(SEED_SUBJECT_ID));
        record.put("record_key", AttributeValue.fromS(SEED_RECORD_KEY));
        record.put("purpose", AttributeValue.fromS(SEED_PURPOSE));
        record.put("value", AttributeValue.fromS("{\"email\":\"demo@example.invalid\"}"));
        record.put("created_at", AttributeValue.fromN(String.valueOf(SEED_TIMESTAMP)));
        record.put("updated_at", AttributeValue.fromN(String.valueOf(SEED_TIMESTAMP)));
        record.put("version", AttributeValue.fromN("1"));
        record.put("retention_days", AttributeValue.fromN("1"));
        record.put("tombstoned", AttributeValue.fromBool(false));
        record.put("request_id", AttributeValue.fromS("seed"));
        dynamo.putItem(PutItemRequest.builder().tableName(RECORDS_TABLE).item(record).build());
    }

    private void clearTable(String table, String... keyNames) {
        Map<String, String> names = new HashMap<>();
        StringBuilder projection = new StringBuilder();
        for (String keyName : keyNames) {
            names.put("#" + keyName, keyName);
            projection.append(projection.length() == 0 ? "" : ",").append("#").append(keyName);
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
