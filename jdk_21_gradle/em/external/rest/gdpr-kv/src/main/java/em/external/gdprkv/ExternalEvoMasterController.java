package em.external.gdprkv;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
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

public class ExternalEvoMasterController extends ExternalSutController {


    public static void main(String[] args) {

        int controllerPort = 40100;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = 12345;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/gdpr-kv/build/libs";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/gdpr-kv-sut.jar";
        }

        int timeoutSeconds = 120;
        if(args.length > 3){
            timeoutSeconds = Integer.parseInt(args[3]);
        }
        String command = "java";
        if(args.length > 4){
            command = args[4];
        }


        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation,
                        sutPort, timeoutSeconds, command);
        controller.setNeedsJdk17Options(true);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private final int timeoutSeconds;
    private final int sutPort;
    private  String jarLocation;

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


    public ExternalEvoMasterController(){
        this(40100, "cs/rest/gdpr-kv/build/libs", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(
            int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command
           ) {

        if(jarLocation==null || jarLocation.isEmpty()){
            throw new IllegalArgumentException("Missing jar location");
        }


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
                "--server.aws.endpoint=" + awsEndpoint,
                "--server.aws.region=" + AWS_REGION,
                "--server.aws.use-localstack=true"
        };
    }

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

    @Override
    public String getLogMessageOfInitializedServer() {
        return "Started GdprKvApplication in";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
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
    }

    @Override
    public void postStart() {
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
    public void preStop() {
    }

    @Override
    public void postStop() {
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
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/v3/api-docs",
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
