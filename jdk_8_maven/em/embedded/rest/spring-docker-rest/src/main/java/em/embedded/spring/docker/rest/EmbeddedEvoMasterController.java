package em.embedded.spring.docker.rest;


import com.abhishekd.restapi.RestApiApplication;
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
import org.testcontainers.containers.GenericContainer;

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

    private static final GenericContainer mysql = new GenericContainer("mysql:8.0" )
            .withEnv(new HashMap<String, String>(){{
                put("MYSQL_ROOT_PASSWORD", "root");
                put("MYSQL_DATABASE", "docker");
            }})
            .withExposedPorts(3306)
            .withTmpFs(Collections.singletonMap("/var/lib/mysql", "rw"))
            ;

    private ConfigurableApplicationContext ctx;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

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
        System.setProperty("ACTIVE_PROFILE", "dev");
        System.setProperty("DB_HOST", host);
        System.setProperty("DB_PORT", String.valueOf(port));
        System.setProperty("DB_NAME", "docker");
        System.setProperty("DB_USER", "root");
        System.setProperty("DB_PASS", "root");

        ctx = SpringApplication.run(RestApiApplication.class, new String[]{
                "--server.port=0"
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

        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.MYSQL,sqlConnection));

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
        return "com.abhishekd.";
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
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
        return SutInfoDto.OutputFormat.JAVA_JUNIT_4;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

}
