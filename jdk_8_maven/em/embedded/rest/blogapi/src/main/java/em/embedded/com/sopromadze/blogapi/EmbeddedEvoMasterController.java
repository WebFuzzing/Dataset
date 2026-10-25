package em.embedded.com.sopromadze.blogapi;


import com.sopromadze.blogapi.BlogApiApplication;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

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

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer mysql = new GenericContainer("mysql:8.0" )
            .withCreateContainerCmdModifier(modifier)
            .withEnv(new HashMap<String, String>(){{
                put("MYSQL_ROOT_PASSWORD", "root");
                put("MYSQL_DATABASE", "blogapi");
            }})
            .withExposedPorts(3306)
            .withTmpFs(Collections.singletonMap("/var/lib/mysql", "rw"))
            .withClasspathResourceMapping("blogapi.sql", "/docker-entrypoint-initdb.d/blogapi.sql", BindMode.READ_ONLY)
            .waitingFor(Wait.forLogMessage(".*MySQL init process done. Ready for start up.*", 1))
            ;

    private ConfigurableApplicationContext ctx;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;
    private String INIT_DB_SCRIPT_PATH = "/data.sql";
    private static final String rawPassword = "bar123";

    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {

        mysql.start();

        String host = mysql.getContainerIpAddress();
        int port = mysql.getMappedPort(3306);
        String url = "jdbc:mysql://"+host+":"+port+"/blogapi?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";

        ctx = SpringApplication.run(BlogApiApplication.class, new String[]{
                "--server.port=0",
                "--spring.datasource.url="+url
        });

        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        JdbcTemplate jdbc = ctx.getBean(JdbcTemplate.class);
        try {
            sqlConnection = jdbc.getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.MYSQL,sqlConnection)
                .withInitSqlOnResourcePath(INIT_DB_SCRIPT_PATH));
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
        mysql.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.sopromadze.blogapi.";
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForJsonTokenBearer(
                        "admin",
                        "/api/auth/signin",
                        "{\"usernameOrEmail\":\"admin\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                ),
                AuthUtils.getForJsonTokenBearer(
                        "foo",
                        "/api/auth/signin",
                        "{\"usernameOrEmail\":\"user\", \"password\":\""+rawPassword+"\"}",
                        "/accessToken"
                )
        );
    }


    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/blogapi.json",
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

}
