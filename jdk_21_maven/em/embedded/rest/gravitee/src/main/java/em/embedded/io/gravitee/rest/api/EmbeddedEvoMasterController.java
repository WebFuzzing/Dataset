package em.embedded.io.gravitee.rest.api;

import com.mongodb.MongoBulkWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.InsertManyOptions;
import io.gravitee.rest.api.standalone.GraviteeApisContainer;
import io.gravitee.rest.api.standalone.boostrap.Bootstrap;
import io.gravitee.rest.api.standalone.jetty.JettyEmbeddedContainer;
import org.bson.Document;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.context.ConfigurableApplicationContext;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "gravitee";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodb = new GenericContainer(MONGODB_IMAGE)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(MONGODB_PORT);

    // config, plugins and templates of the distribution (~500 MB), unpacked once per JVM
    private static String graviteeHome;

    public static void main(String[] args) {

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private GraviteeApisContainer container;

    private MongoClient mongoClient;

    // DB content right after startup (default organization, environment, roles...)
    private Map<String, List<Document>> snapshot;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {

        mongodb.start();
        mongoClient = MongoClients.create("mongodb://" + mongodb.getHost() + ":" + mongodb.getMappedPort(MONGODB_PORT));

        if (graviteeHome == null) {
            graviteeHome = Bootstrap.unpackEmbeddedHome();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteRecursively(Path.of(graviteeHome))));
        }
        System.setProperty("gravitee.home", graviteeHome);
        // a fresh dir on each start: on Windows the previous start keeps its unzipped plugin jars locked
        System.setProperty("plugins.workDir", graviteeHome + "/plugins/.work-" + System.nanoTime());
        System.setProperty("jetty.port", "0");
        System.setProperty("ds.mongodb.host", mongodb.getHost());
        System.setProperty("ds.mongodb.port", String.valueOf(mongodb.getMappedPort(MONGODB_PORT)));
        System.setProperty("repositories.analytics.type", "none");
        System.setProperty("services.core.http.enabled", "false");

        container = new GraviteeApisContainer();
        try {
            container.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        snapshot = new HashMap<>();
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            snapshot.put(name, db.getCollection(name).find().into(new ArrayList<>()));
        }

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        try {
            JettyEmbeddedContainer jetty = container.applicationContext().getBean(JettyEmbeddedContainer.class);
            Field server = JettyEmbeddedContainer.class.getDeclaredField("server");
            server.setAccessible(true);
            return ((ServerConnector) ((Server) server.get(jetty)).getConnectors()[0]).getLocalPort();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isSutRunning() {
        return container != null && ((ConfigurableApplicationContext) container.applicationContext()).isActive();
    }

    @Override
    public void stopSut() {
        if (container != null) {
            try {
                container.stop();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            container = null;
        }
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        mongodb.stop();
    }

    private static void deleteRecursively(Path dir) {
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException ignored) {
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
                "http://localhost:" + getSutPort() + "/management/openapi.json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
