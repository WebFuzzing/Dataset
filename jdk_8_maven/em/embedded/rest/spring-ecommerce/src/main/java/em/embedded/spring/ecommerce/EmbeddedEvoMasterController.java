package em.embedded.spring.ecommerce;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
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
import org.testcontainers.utility.DockerImageName;
import com.techie.shoppingstore.NgSpringShoppingStoreApplication;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.bson.types.ObjectId;

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

    private static final String MONGODB_VERSION = "7.0";

    private static final String MONGODB_DATABASE_NAME = "test";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodbContainer = new GenericContainer("mongo:" + MONGODB_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withExposedPorts(MONGODB_PORT);

    private static final String REDIS_VERSION = "7.0.11";
    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> redisContainer = new GenericContainer("redis:" + REDIS_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(REDIS_PORT)
            .withCommand("redis-server", "--appendonly", "yes");

    private static final String ELASTICSEARCH_VERSION = "6.8.23";
    private static final int HTTP_PORT = 9200;
    private static final int TRANSPORT_PORT = 9300;

    private static final GenericContainer<?> elasticsearchContainer =
            new GenericContainer<>(DockerImageName.parse(
                    "docker.elastic.co/elasticsearch/elasticsearch:" + ELASTICSEARCH_VERSION))
                    .withCreateContainerCmdModifier(modifier)
                    .withEnv("discovery.type", "single-node")
                    .withEnv("cluster.name", "elasticsearch")
                    .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                    .withEnv("xpack.security.enabled", "false")
                    .withTmpFs(Collections.singletonMap("/usr/share/elasticsearch/data", "rw"))
                    .withExposedPorts(HTTP_PORT, TRANSPORT_PORT);


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
        redisContainer.start();
        elasticsearchContainer.start();

        mongoClient = MongoClients.create("mongodb://" + mongodbContainer.getContainerIpAddress() + ":" + mongodbContainer.getMappedPort(MONGODB_PORT));

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        ctx = SpringApplication.run(NgSpringShoppingStoreApplication.class,
                new String[]{"--server.port=0",
                        "--spring.datasource.host=" + mongodbContainer.getContainerIpAddress(),
                        "--spring.datasource.port=" + mongodbContainer.getMappedPort(MONGODB_PORT),
                        "--spring.datasource.database=" + MONGODB_DATABASE_NAME,
                        "--spring.data.mongodb.uri=mongodb://" + mongodbContainer.getContainerIpAddress() + ":" + mongodbContainer.getMappedPort(MONGODB_PORT) + "/" + MONGODB_DATABASE_NAME,
                        "--spring.redis.host=" + redisContainer.getContainerIpAddress(),
                        "--spring.redis.port=" + redisContainer.getMappedPort(REDIS_PORT),
                        "--spring.data.elasticsearch.cluster-name=elasticsearch",
                        "--spring.data.elasticsearch.cluster-nodes=" + elasticsearchContainer.getContainerIpAddress() + ":" + elasticsearchContainer.getMappedPort(TRANSPORT_PORT),
                        "--spring.elasticsearch.rest.uris=" + elasticsearchContainer.getContainerIpAddress() + ":" + elasticsearchContainer.getMappedPort(HTTP_PORT),
                        "--spring.cache.type=NONE",
                        "--spring.data.elasticsearch.host=" + elasticsearchContainer.getContainerIpAddress(),
                        "--spring.data.elasticsearch.port=" + elasticsearchContainer.getMappedPort(TRANSPORT_PORT)
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

        mongodbContainer.stop();
        mongoClient.close();
        redisContainer.stop();
        elasticsearchContainer.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.techie.shoppingstore.";
    }

    @Override
    public void resetStateOfSUT() {
        MongoDatabase db = mongoClient.getDatabase(MONGODB_DATABASE_NAME);


        for(String name: db.listCollectionNames()){
            db.getCollection(name).deleteMany(new BasicDBObject());
        }

        MongoCollection<Document> users = db.getCollection("User");
        users.insertMany(Arrays.asList(
                new Document()
                        .append("_id", new ObjectId())
                        .append("_class", "com.techie.shoppingstore.model.User")
                        .append("username", "user1")
                        .append("email", "user1@email.com")
                        .append("enabled", true)
                        //12345678
                        .append("password", "$2a$12$p9eP3beaPuSMbS1enDn1Z.zFuv6npjm6xjyQnnEqvVG.CD03d1aoi"),
                new Document()
                        .append("_id", new ObjectId())
                        .append("_class", "com.techie.shoppingstore.model.User")
                        .append("username", "user2")
                        .append("email", "user2@email.com")
                        .append("enabled", true)
                        //12345678
                        .append("password", "$2a$12$p9eP3beaPuSMbS1enDn1Z.zFuv6npjm6xjyQnnEqvVG.CD03d1aoi")
        ));
    }


    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    String rawPassword = "12345678";

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForJsonTokenBearer(
                        "user1",
                        "/api/auth/login",
                        "{\"username\":\"user1\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                ),
                AuthUtils.getForJsonTokenBearer(
                        "user2",
                        "/api/auth/login",
                        "{\"username\":\"user2\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                )
        );
    }




    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }
}
