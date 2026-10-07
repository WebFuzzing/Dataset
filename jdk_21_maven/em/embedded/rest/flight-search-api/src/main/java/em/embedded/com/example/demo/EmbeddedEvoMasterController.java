package em.embedded.com.example.demo;

import com.example.demo.FlightSearchApiApplication;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.evomaster.client.java.controller.AuthUtils;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class used to start/stop the SUT.
 */
public class EmbeddedEvoMasterController extends EmbeddedSutController {

    private static final String MONGODB_IMAGE = "mongo:7";

    private static final int MONGODB_PORT = 27017;

    private static final String MONGODB_DATABASE = "flightdatabase";

    // The DB starts empty (the flight loader is never scheduled); airports and flights can be created via the API
    private static final List<Document> USERS = List.of(
            user("wfd_admin1", "$2a$10$2xOaY0XmjU8VrHnQ6ORmme.4pAD21fsnoM9dcf8Y8NYwmXZztsDsC", "ADMIN"),
            user("wfd_admin2", "$2a$10$qKrL3SboXVhfaaevAMiU.eFAj1wD4.X2VVvI.XSmjVlTPf8.95Z9y", "ADMIN"),
            user("wfd_user1", "$2a$10$t.OcuL3NBcdCtRCUtX4SiuG6RPgGxs0yQpdvVl11J0Mv249/vu9Za", "USER"),
            user("wfd_user2", "$2a$10$1WlQAEEMP97IO1o8vLl3ROrLeF9hdKRjd0yxlOve.ZhF.HCW9rES.", "USER")
    );

    private static Document user(String id, String passwordHash, String type) {
        return new Document("_id", id)
                .append("EMAIL", id + "@wfd.invalid")
                .append("PASSWORD", passwordHash)
                .append("FIRST_NAME", id)
                .append("LAST_NAME", id)
                .append("PHONE_NUMBER", "05550000000")
                .append("USER_TYPE", type)
                .append("USER_STATUS", "ACTIVE")
                .append("_class", "com.example.demo.auth.model.entity.UserEntity");
    }

    private static final GenericContainer mongodb = new GenericContainer(MONGODB_IMAGE)
            .withExposedPorts(MONGODB_PORT);

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

    private MongoClient mongoClient;

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
        seedUsers();

        ctx = SpringApplication.run(FlightSearchApiApplication.class, new String[]{
                "--server.port=0",
                "--spring.data.mongodb.host=" + mongodb.getHost(),
                "--spring.data.mongodb.port=" + mongodb.getMappedPort(MONGODB_PORT),
                "--spring.data.mongodb.database=" + MONGODB_DATABASE
        });

        return "http://localhost:" + getSutPort();
    }

    private void seedUsers() {
        List<Document> users = new ArrayList<>();
        for (Document u : USERS) {
            users.add(new Document(u));
        }
        mongoClient.getDatabase(MONGODB_DATABASE).getCollection("user-collection").insertMany(users);
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
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
        }
        mongodb.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.example.demo.";
    }

    @Override
    public void resetStateOfSUT() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE);
        for (String name : db.listCollectionNames()) {
            db.getCollection(name).deleteMany(new Document());
        }
        seedUsers();
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    private static AuthenticationDto login(String name, String email, String password) {
        return AuthUtils.getForJsonTokenBearer(
                name,
                "/api/v1/authentication/user/login",
                "{\"email\": \"" + email + "\", \"password\": \"" + password + "\"}",
                "/response/accessToken"
        );
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of(
                login("WfdAdmin1", "wfd_admin1@wfd.invalid", "Wfd-Admin-Pass1"),
                login("WfdAdmin2", "wfd_admin2@wfd.invalid", "Wfd-Admin-Pass2"),
                login("WfdUser1", "wfd_user1@wfd.invalid", "Wfd-User-Pass1"),
                login("WfdUser2", "wfd_user2@wfd.invalid", "Wfd-User-Pass2")
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
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

}
