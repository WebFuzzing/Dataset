package em.external.reciter;

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
        String jarLocation = "cs/rest/reciter/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/reciter-sut.jar";
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

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer<?> localstack = new GenericContainer<>(LOCALSTACK_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withEnv("SERVICES", "dynamodb")
            .withEnv("DEFAULT_REGION", AWS_REGION)
            .withExposedPorts(LOCALSTACK_PORT)
            .withStartupTimeout(Duration.ofMinutes(3));

    private final int timeoutSeconds;
    private final int sutPort;
    private String jarLocation;

    private int localstackMappedPort;
    private DynamoDbClient dynamo;
    private final Map<String, List<String>> tableKeys = new LinkedHashMap<>();

    public ExternalEvoMasterController() {
        this(40100, "cs/rest/reciter/target/reciter-sut.jar", 12345, 120, "java");
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
        String logFile = Path.of(System.getProperty("java.io.tmpdir"), "reciter-sut-" + sutPort + ".log").toString();
        return new String[]{
                "--server.port=" + sutPort,
                "--logging.file.name=" + logFile,
                "--springdoc.api-docs.version=OPENAPI_3_0",
                "--spring.security.enabled=false",
                "--aws.dynamoDb.local=true",
                "--aws.dynamoDb.local.port=" + localstackMappedPort,
                "--aws.dynamoDb.local.region=" + AWS_REGION,
                "--aws.dynamoDb.local.accesskey=test",
                "--aws.dynamoDb.local.secretkey=test",
                "--aws.dynamodb.settings.table.create=true",
                "--aws.s3.use=false",
                "--aws.s3.use.cached.identityAll=false"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{
                "-Daws.accessKeyId=test",
                "-Daws.secretAccessKey=test"
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
        return "Started Application in";
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
        dynamo = DynamoDbClient.builder()
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.of(AWS_REGION))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();

        awaitDynamoDb();
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
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        tableKeys.clear();
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
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/reciter/v3/api-docs",
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
