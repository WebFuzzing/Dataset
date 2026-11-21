package em.external.arcane.dam;

import com.github.dockerjava.api.model.Ulimit;
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
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        String jarLocation = "cs/rest/arcane-dam-server/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/arcane-dam-server-sut.jar";
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

    private static final String LOCALSTACK_IMAGE = "localstack/localstack:4.4";

    private static final int LOCALSTACK_PORT = 4566;

    private static final String AWS_REGION = "us-east-1";

    private static final String S3_BUCKET = "arcane-bucket";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer<?> localstack = new GenericContainer<>(LOCALSTACK_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withEnv("SERVICES", "dynamodb,s3")
            .withEnv("DEFAULT_REGION", AWS_REGION)
            .withExposedPorts(LOCALSTACK_PORT)
            .withStartupTimeout(Duration.ofMinutes(3));

    private final int timeoutSeconds;
    private final int sutPort;
    private String jarLocation;

    private int localstackMappedPort;
    private DynamoDbClient dynamo;
    private S3Client s3;
    private final Map<String, List<String>> tableKeys = new LinkedHashMap<>();

    public ExternalEvoMasterController() {
        this(40100, "cs/rest/arcane-dam-server/target/arcane-dam-server-sut.jar", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command) {
        if (jarLocation == null || jarLocation.isEmpty()) {
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
        String logFile = Path.of(System.getProperty("java.io.tmpdir"), "arcane-dam-server-sut-" + sutPort + ".log").toString();
        return new String[]{
                "--server.port=" + sutPort,
                "--logging.file.name=" + logFile,
                "--spring.config.additional-location=classpath:/benchmark/application.properties",
                "--aws.endpoint=http://" + localstack.getHost() + ":" + localstackMappedPort,
                "--aws.region=" + AWS_REGION,
                "--aws.access_key=test",
                "--aws.secret_key=test",
                "--aws.s3.bucket-name=" + S3_BUCKET,
                "--app.security.enabled=false",
                "--springdoc.api-docs.version=OPENAPI_3_0"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[0];
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
        return "Started ArcaneDamServerApplication in";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        tableKeys.clear();
        localstack.start();
        localstackMappedPort = localstack.getMappedPort(LOCALSTACK_PORT);
        String awsEndpoint = "http://" + localstack.getHost() + ":" + localstackMappedPort;

        StaticCredentialsProvider creds = StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"));
        dynamo = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(AWS_REGION))
                .credentialsProvider(creds)
                .build();

        s3 = S3Client.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(AWS_REGION))
                .credentialsProvider(creds)
                .forcePathStyle(true)
                .build();

        awaitDynamoDb();
        ensureInfrastructure();
    }

    @Override
    public void postStart() {
        cacheTableKeys();
    }

    @Override
    public void resetStateOfSUT() {
        if (tableKeys.isEmpty()) {
            cacheTableKeys();
        }
        for (Map.Entry<String, List<String>> entry : tableKeys.entrySet()) {
            clearTable(entry.getKey(), entry.getValue());
        }
        clearBucket();
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        tableKeys.clear();
        if (s3 != null) {
            s3.close();
            s3 = null;
        }
        if (dynamo != null) {
            dynamo.close();
            dynamo = null;
        }
        localstack.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.arcane.dam.";
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

    @Override
    public Object getDynamoDbConnection() {
        return dynamo;
    }

    private void awaitDynamoDb() {
        long deadline = System.currentTimeMillis() + 120_000;
        while (true) {
            try {
                dynamo.listTables();
                s3.listBuckets();
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

    private void ensureInfrastructure() {
        createHashTableIfAbsent("asset", "asset_id");
        createHashTableIfAbsent("team", "id");
        createHashTableIfAbsent("users", "id");

        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(S3_BUCKET).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException ignored) {
        }
    }

    private void createHashTableIfAbsent(String tableName, String hashKeyName) {
        try {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName(hashKeyName)
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName(hashKeyName)
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
        } catch (ResourceInUseException ignored) {
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

    private void clearBucket() {
        if (s3 == null) {
            return;
        }
        ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(S3_BUCKET).build();
        for (var page : s3.listObjectsV2Paginator(listReq)) {
            for (S3Object obj : page.contents()) {
                s3.deleteObject(DeleteObjectRequest.builder().bucket(S3_BUCKET).key(obj.key()).build());
            }
        }
    }
}
