package em.embedded.reservationsapi;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;
import sk.cyrilgavala.reservationsApi.ReservationsApi;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;


/**
 * Class used to start/stop the SUT. This will be controller by the EvoMaster process
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

    private static final int MONGODB_PORT = 27017;

    //https://www.mongodb.com/docs/drivers/java/sync/current/compatibility/
    private static final String MONGODB_VERSION = "4.4";

    private static final String MONGODB_DATABASE_NAME = "reservations-api";

    //  docker run -p 27017:27017  -e MONGODB_REPLICA_SET_MODE=primary -e  ALLOW_EMPTY_PASSWORD=yes bitnami/mongodb:4.4
    //  https://hub.docker.com/r/bitnami/mongodb
    // cannot use standard Mongo image, due ridiculous handling of transaction that requires a cluster...

//    private static final GenericContainer mongodbContainer = new GenericContainer("bitnami/mongodb:" + MONGODB_VERSION)
  //          .withTmpFs(Collections.singletonMap("/bitnami/mongodb", "rw"))
    //        .withEnv("MONGODB_REPLICA_SET_MODE", "primary")
      //      .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
        //    .withExposedPorts(MONGODB_PORT);

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodbContainer = new GenericContainer("mongo:" + MONGODB_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withEnv("MONGODB_REPLICA_SET_MODE", "primary")
            .withEnv("ALLOW_EMPTY_PASSWORD", "yes")
            .withCommand("--replSet rs0")
            .withExposedPorts(MONGODB_PORT);


    private static final String rawPassword = "bar123";
    private static final String hashedPassword = "$2a$10$nEDY5j731yXGnQHyM39PWurJWr1FukegmKYYarK5WOoAMmgDs6D3u";

    private String mongoDbUrl;

    private MongoClient mongoClient;

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        mongodbContainer.start();
        mongoDbUrl = "mongodb://" + mongodbContainer.getContainerIpAddress() + ":" + mongodbContainer.getMappedPort(MONGODB_PORT) + "/" + MONGODB_DATABASE_NAME;
        mongoClient = MongoClients.create(mongoDbUrl);

        mongoClient.getDatabase("admin")
                .runCommand(new Document("replSetInitiate", new Document()));

        /*
         This parameter is related to "cs/rest/reservations-api/src/main/java/sk/cyrilgavala/reservationsApi/web/controller/UserRestController.java" class.
         In this class, after the password received as input is decoded, a new String is created. While creating this string, the current computer's default Charset is used and may exhibit different behaviors on different computers. To prevent this, we define a charset.
         */
        System.setProperty("file.encoding", "ISO-8859-1");

        ctx = SpringApplication.run(ReservationsApi.class,
                new String[]{"--server.port=0",
                        "--databaseUrl=" + mongoDbUrl,
                        "--spring.data.mongodb.uri=" + mongoDbUrl,
                        "--spring.cache.type=NONE",
                        "--app.jwt.secret=abcdef012345678901234567890123456789abcdef012345678901234567890123456789"
                });

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            // do nothing
        }

        while (!isMongoClientReady()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        return "http://localhost:" + getSutPort();
    }

    /**
     * Checks if the mongo database is ready to receive commands using a ping command
     * @return
     */
    private boolean isMongoClientReady() {
        try {
            MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE_NAME);
            Document pingResult = db.runCommand(new Document("ping", 1));
            return pingResult.getDouble("ok") == 1.0;
        } catch (Exception ex) {
            // Connection error
            return false;
        }
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

        mongodbContainer.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "sk.cyrilgavala.reservationsApi.";
    }

    @Override
    public void resetStateOfSUT() {

        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE_NAME);

        //THIS WAS VERY EXPENSIVE for this API... might be due to transactions or different Docker image?
        //db.drop();

        for(String name: db.listCollectionNames()){
            db.getCollection(name).deleteMany(new BasicDBObject());
        }

        MongoCollection<Document> users = db.getCollection("users");
        users.insertMany(Arrays.asList(
                new Document()
                        .append("_id", new ObjectId())
                        .append("_class", "sk.cyrilgavala.reservationsApi.model.User")
                        .append("username", "foo")
                        .append("email", "foo@foo.com")
                        .append("password", hashedPassword)
                        .append("role", "USER"),
                new Document()
                        .append("_id", new ObjectId())
                        .append("_class", "sk.cyrilgavala.reservationsApi.model.User")
                        .append("username", "bar")
                        .append("email", "bar@foo.com")
                        .append("password", hashedPassword)
                        .append("role", "USER"),
                new Document()
                        .append("_id", new ObjectId())
                        .append("_class", "sk.cyrilgavala.reservationsApi.model.User")
                        .append("username", "admin")
                        .append("email", "admin@foo.com")
                        .append("password", hashedPassword)
                        .append("role", "ADMIN")
        ));
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForJsonTokenBearer(
                        "admin",
                        "/api/user/login",
                        "{\"username\":\"admin\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                ),
                AuthUtils.getForJsonTokenBearer(
                        "foo",
                        "/api/user/login",
                        "{\"username\":\"foo\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                ),
                AuthUtils.getForJsonTokenBearer(
                        "bar",
                        "/api/user/login",
                        "{\"username\":\"bar\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                )
        );
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
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

}
