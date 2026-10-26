package em.external.spring.ecommerce;

import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        String jarLocation = "cs/rest/original/spring-ecommerce/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/spring-ecommerce-sut.jar";
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
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private final int timeoutSeconds;
    private final int sutPort;
    private  String jarLocation;

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


    public ExternalEvoMasterController(){
        this(40100, "../core/target", 12345, 120, "java");
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
         };
    }

    public String[] getJVMParameters() {

        return new String[]{
                "-Dfile.encoding=ISO-8859-1"
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
        return "Started NgSpringShoppingStoreApplication in ";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {

        mongodbContainer.start();
        redisContainer.start();
        elasticsearchContainer.start();

        mongoClient = MongoClients.create("mongodb://" + mongodbContainer.getContainerIpAddress() + ":" + mongodbContainer.getMappedPort(MONGODB_PORT));

    }

    @Override
    public void postStart() {
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
    public void preStop() {
    }

    @Override
    public void postStop() {
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
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/v2/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
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
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public Object getMongoConnection() {
        return mongoClient;
    }


}
