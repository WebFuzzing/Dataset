package em.embedded.org.movies.xml;

import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.movies.xml.MoviesXmlApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {

        ctx = SpringApplication.run(MoviesXmlApplication.class, new String[]{
                "--server.port=0",
                "--logging.level.root=OFF",
                "--logging.level.org.springframework=INFO"
        });

        closeDataBaseConnection();
        try {
            sqlConnection = ctx.getBean(JdbcTemplate.class).getDataSource().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.H2, sqlConnection));

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
        closeDataBaseConnection();
        ctx.stop();
        ctx.close();
    }

    private void closeDataBaseConnection() {
        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sqlConnection = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.movies.xml.";
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return null;
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
