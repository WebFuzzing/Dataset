package em.external.it.pagopa.pn.mandate;

import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs the upstream pn-mandate service against an isolated LocalStack instance. */
public class ExternalEvoMasterController extends ExternalSutController {

    private static final String APP_NAME = "pn-mandate";
    private static final String INITIALIZED_MESSAGE = "Started MandateApplication in";
    private static final String PACKAGE_PREFIX = "it.pagopa.pn.mandate.,it.pagopa.pn.ciechecker.";
    private static final String SERVICES = "dynamodb,sqs,ssm,s3";
    private static final String CSCA_BUCKET = "pn-runtime-environment-variables-eu-south-1-830192246553";
    private static final String CSCA_KEY = "pn-mandate/csca-masterlist/new_IT_MasterListCSCA.zip";

    private final int sutPort;
    private final int timeoutSeconds;
    private final String jarLocation;
    private final GenericContainer<?> localstack;
    private final Map<String, List<Map<String, AttributeValue>>> initialItems = new LinkedHashMap<>();

    private DynamoDbClient dynamo;
    private String awsEndpoint;
    private String schemaLocation;

    /** Starts the controller from the benchmark runner jar. */
    public static void main(String[] args) {
        int controllerPort = args.length > 0 ? Integer.parseInt(args[0]) : 40100;
        int sutPort = args.length > 1 ? Integer.parseInt(args[1]) : 12345;
        String jar = args.length > 2 ? args[2] : "cs/rest/pn-mandate/target/pn-mandate-sut.jar";
        int timeout = args.length > 3 ? Integer.parseInt(args[3]) : 180;
        String java = args.length > 4 ? args[4] : "java";
        ExternalEvoMasterController controller = new ExternalEvoMasterController(controllerPort, sutPort, jar, timeout, java);
        controller.setNeedsJdk17Options(true);
        new InstrumentedSutStarter(controller).start();
    }

    /** Creates a controller with explicit process settings. */
    public ExternalEvoMasterController(int controllerPort, int sutPort, String jarLocation, int timeoutSeconds, String java) {
        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        this.localstack = new GenericContainer<>("localstack/localstack:4.4")
                .withEnv("SERVICES", SERVICES)
                .withEnv("AWS_ACCESS_KEY_ID", "test")
                .withEnv("AWS_SECRET_ACCESS_KEY", "test")
                .withExposedPorts(4566)
                .withStartupTimeout(Duration.ofMinutes(3));
        setControllerPort(controllerPort);
        setJavaCommand(java);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--spring.config.additional-location=classpath:/benchmark/application.properties",
                "--logging.config=classpath:logback-base.xml",
                "--pn.middleware.init.aws=true",
                "--aws.profile-name=",
                "--spring.cloud.aws.credentials.profile-name=",
                "--spring.cloud.aws.credentials.profile.name=",
                "--spring.cloud.aws.credentials.access-key=test",
                "--spring.cloud.aws.credentials.secret-key=test",
                "--aws.region-code=us-east-1",
                "--spring.cloud.aws.region.static=us-east-1",
                "--aws.endpoint-url=" + awsEndpoint,
                "--spring.cloud.aws.endpoint=" + awsEndpoint,
                "--spring.cloud.aws.sqs.endpoint=" + awsEndpoint
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{"-Daws.accessKeyId=test", "-Daws.secretAccessKey=test"};
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
        return INITIALIZED_MESSAGE;
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        localstack.start();
        awsEndpoint = "http://" + localstack.getHost() + ":" + localstack.getMappedPort(4566);
        runUpstreamInitialization();
        uploadCscaMasterlist();
        dynamo = DynamoDbClient.builder()
                .endpointOverride(URI.create(awsEndpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
        for (String table : dynamo.listTables().tableNames()) {
            initialItems.put(table, scanAll(table));
        }
    }

    @Override
    public void postStart() {
    }

    @Override
    public void resetStateOfSUT() {
        for (Map.Entry<String, List<Map<String, AttributeValue>>> entry : initialItems.entrySet()) {
            String table = entry.getKey();
            List<String> keys = new ArrayList<>();
            dynamo.describeTable(r -> r.tableName(table)).table().keySchema()
                    .forEach(key -> keys.add(key.attributeName()));
            for (Map<String, AttributeValue> item : scanAll(table)) {
                Map<String, AttributeValue> key = new HashMap<>();
                for (String name : keys) {
                    key.put(name, item.get(name));
                }
                dynamo.deleteItem(DeleteItemRequest.builder().tableName(table).key(key).build());
            }
            for (Map<String, AttributeValue> item : entry.getValue()) {
                dynamo.putItem(PutItemRequest.builder().tableName(table).item(item).build());
            }
        }
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
        localstack.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return PACKAGE_PREFIX;
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

    /** Executes the upstream test table and queue setup with container-local addresses. */
    private void runUpstreamInitialization() {
        try (InputStream in = getClass().getResourceAsStream("/init-localstack.sh")) {
            if (in == null) {
                throw new IllegalStateException("Missing LocalStack initialization script");
            }
            String script = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("\r\n", "\n")
                    .replace("--profile default", "")
                    .replace("http://localstack:4566", "http://localhost:4566")
                    .replace("KeyType=SORT", "KeyType=RANGE");
            Path file = Files.createTempFile(APP_NAME + "-init", ".sh");
            try {
                Files.writeString(file, script);
                localstack.copyFileToContainer(MountableFile.forHostPath(file), "/tmp/init.sh");
                Container.ExecResult result = localstack.execInContainer("sh", "/tmp/init.sh");
                if (result.getExitCode() != 0 || result.getStdout().contains("An error occurred")) {
                    throw new IllegalStateException("LocalStack setup failed: " + result.getStdout() + result.getStderr());
                }
            } finally {
                Files.deleteIfExists(file);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot prepare LocalStack", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LocalStack setup interrupted", e);
        }
    }

    /** Uploads the CSCA masterlist zip to LocalStack S3 before the SUT starts. */
    private void uploadCscaMasterlist() {
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

            Container.ExecResult mb = localstack.execInContainer("awslocal", "s3", "mb", "s3://" + CSCA_BUCKET);
            if (mb.getExitCode() != 0 && !mb.getStderr().contains("BucketAlreadyOwnedByYou")) {
                throw new IllegalStateException("S3 bucket creation failed: " + mb.getStdout() + mb.getStderr());
            }

            Container.ExecResult cp = localstack.execInContainer("awslocal", "s3", "cp",
                    "/tmp/new_IT_MasterListCSCA.zip",
                    "s3://" + CSCA_BUCKET + "/" + CSCA_KEY);
            if (cp.getExitCode() != 0) {
                throw new IllegalStateException("S3 CSCA zip upload failed: " + cp.getStdout() + cp.getStderr());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot upload CSCA masterlist zip to LocalStack S3", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LocalStack S3 initialization interrupted", e);
        }
    }

    /** Reads every page so a reset handles large test runs. */
    private List<Map<String, AttributeValue>> scanAll(String table) {
        List<Map<String, AttributeValue>> items = new ArrayList<>();
        Map<String, AttributeValue> startKey = null;
        do {
            ScanRequest.Builder scan = ScanRequest.builder().tableName(table);
            if (startKey != null && !startKey.isEmpty()) {
                scan.exclusiveStartKey(startKey);
            }
            var page = dynamo.scan(scan.build());
            items.addAll(page.items());
            startKey = page.lastEvaluatedKey();
        } while (startKey != null && !startKey.isEmpty());
        return items;
    }

    /** Materializes the OpenAPI file on disk for schema resolution. */
    private synchronized String schemaLocation() {
        if (schemaLocation == null) {
            try {
                Path dir = Files.createTempDirectory(APP_NAME + "-openapi");
                for (String name : List.of(APP_NAME + ".yaml")) {
                    try (InputStream in = getClass().getResourceAsStream("/" + name)) {
                        if (in == null) {
                            throw new IllegalStateException("Missing OpenAPI resource: " + name);
                        }
                        Files.copy(in, dir.resolve(name));
                    }
                    dir.resolve(name).toFile().deleteOnExit();
                }
                dir.toFile().deleteOnExit();
                schemaLocation = dir.resolve(APP_NAME + ".yaml").toUri().toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return schemaLocation;
    }
}
