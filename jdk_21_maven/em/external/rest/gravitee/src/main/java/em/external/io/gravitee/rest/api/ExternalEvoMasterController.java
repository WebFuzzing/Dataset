package em.external.io.gravitee.rest.api;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import org.bson.Document;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "gravitee";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodb = new GenericContainer(MONGODB_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(MONGODB_PORT);

    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/gravitee/gravitee-apim-rest-api/gravitee-apim-rest-api-standalone/gravitee-apim-rest-api-standalone-distribution/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/gravitee-sut.jar";
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

    private MongoClient mongoClient;

    // DB content right after startup (default organization, environment, roles...)
    private Map<String, List<Document>> snapshot;

    // the SUT unpacks its ~500 MB distribution here, and a killed process cannot clean it up
    private Path tmpDir;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/gravitee-sut.jar", DEFAULT_SUT_PORT, 120, "java");
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
        return new String[]{
                "--jetty.port=" + sutPort,
                "--ds.mongodb.host=" + mongodb.getHost(),
                "--ds.mongodb.port=" + mongodb.getMappedPort(MONGODB_PORT),
                "--repositories.analytics.type=none",
                "--services.core.http.enabled=false"
        };
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{"-Djava.io.tmpdir=" + tmpDir};
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
        return "Gravitee.io - Rest APIs id[";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        mongodb.start();
        mongoClient = MongoClients.create("mongodb://" + mongodb.getHost() + ":" + mongodb.getMappedPort(MONGODB_PORT));
        try {
            tmpDir = Files.createTempDirectory("gravitee-sut");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void postStart() {
        snapshot = new HashMap<>();
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            snapshot.put(name, db.getCollection(name).find().into(new ArrayList<>()));
        }
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        mongodb.stop();
        if (tmpDir != null) {
            try (Stream<Path> paths = Files.walk(tmpDir)) {
                paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            tmpDir = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "io.gravitee.rest.api.,io.gravitee.apim.";
    }

    @Override
    public void resetStateOfSUT() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            db.getCollection(name).deleteMany(new Document());
        }
        snapshot.forEach((name, docs) -> {
            if (!docs.isEmpty()) {
                try {
                    db.getCollection(name).insertMany(docs, new InsertManyOptions().ordered(false));
                } catch (MongoBulkWriteException e) {
                    // background tasks (e.g. node monitoring) may have written the same document meanwhile
                    if (e.getWriteErrors().stream().anyMatch(w -> w.getCode() != 11000)) {
                        throw e;
                    }
                }
            }
        });
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    // in-memory accounts of the SUT's own gravitee.yml
    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of(
                AuthUtils.getForBasic("Admin", "admin", "admin"),
                AuthUtils.getForBasic("User", "user", "password"),
                AuthUtils.getForBasic("Application1", "application1", "application1"),
                AuthUtils.getForBasic("Api1", "api1", "api1")
        );
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/management/openapi.json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
