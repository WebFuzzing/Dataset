package em.embedded.org.devgateway.ocvn;


import com.mongodb.MongoClient;
import org.devgateway.toolkit.web.spring.WebApplication;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.SqlScriptRunnerCached;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.SQLException;
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
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;
    private MongoClient mongoClient;


    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mongodb = new GenericContainer("mongo:3.2")
            .withCreateContainerCmdModifier(modifier)
            .withTmpFs(Collections.singletonMap("/data/db", "rw"))
            .withExposedPorts(27017);


    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        mongodb.start();

        mongoClient = new MongoClient(mongodb.getContainerIpAddress(),
                mongodb.getMappedPort(27017));

        ctx = SpringApplication.run(WebApplication.class,
                new String[]{"--server.port=0",
                        "--liquibase.enabled=false",
                        "--spring.data.mongodb.uri=mongodb://"+mongodb.getContainerIpAddress()+":"+mongodb.getMappedPort(27017)+"/ocvn",
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;",
                        "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                        "--spring.jpa.properties.hibernate.enable_lazy_load_no_trans=true",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password",
                        "--dg-toolkit.derby.port=0",
                        "--spring.cache.type=NONE",
                        "--spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"
                });



        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);

        try {
            sqlConnection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.H2,sqlConnection)
                .withInitSqlOnResourcePath("/init_db.sql"));

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

        mongodb.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.devgateway.";
    }

    @Override
    public void resetStateOfSUT() {
        mongoClient.getDatabase("ocvn").drop();
        mongoClient.getDatabase("ocvn-shadow").drop();

//        DbCleaner.clearDatabase_H2(connection);
//        SqlScriptRunnerCached.runScriptFromResourceFile(connection,"/init_db.sql");
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {

        return Arrays.asList(
                AuthUtils.getForDefaultSpringFormLogin("ADMIN", "admin", "admin"),
                AuthUtils.getForDefaultSpringFormLogin("user1", "user1", "password"));
    }



    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs?group=1ocDashboardsApi",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

    @Override
    public Object getMongoConnection() {return mongoClient;}
}
