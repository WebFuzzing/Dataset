package em.embedded.swagger.petstore;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleState;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.sql.DbSpecification;


import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import io.swagger.petstore.Main;
import java.util.List;

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

    Main application;

    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {
        application = new Main();

        try {
            application.main(new String[]{
                    "8080"
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return application.getTomcat().getServer().getPort();
    }


    @Override
    public boolean isSutRunning() {
        if (application != null) {
            LifecycleState state = application.getTomcat().getServer().getState();
            return state == LifecycleState.STARTED;
        }
        return false;
    }

    @Override
    public void stopSut() {
        try {
            application.getTomcat().stop();
        } catch (LifecycleException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "io.swagger.petstore.";
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
                "http://localhost:" + getSutPort() + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

}
