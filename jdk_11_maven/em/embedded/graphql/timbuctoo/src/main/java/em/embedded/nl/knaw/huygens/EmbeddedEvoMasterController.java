package em.embedded.nl.knaw.huygens;


import io.dropwizard.configuration.ResourceConfigurationSourceProvider;
import io.dropwizard.setup.Bootstrap;
import nl.knaw.huygens.timbuctoo.server.TimbuctooConfiguration;
import nl.knaw.huygens.timbuctoo.server.TimbuctooV4;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.GraphQlProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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


    private TimbuctooV4 application;

    private String tmpFolder = "tmp";

    public EmbeddedEvoMasterController() {
        this(40100);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer elasticsearch = new GenericContainer("elasticsearch:5.6.5")
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(9200)
            //.withEnv("","")
            //.withTmpFs(Collections.singletonMap("", "rw"))
            ;


    @Override
    public String startSut() {

        elasticsearch.start();

        //application = new TimbuctooV4();
        application = new TimbuctooResource();

        tmpFolder = "tmpFolder";

        resetStateOfSUT();

        System.setProperty("dw.server.applicationConnectors[0].port", "0");
        System.setProperty("dw.securityConfiguration.localAuthentication.authorizationsPath", tmpFolder + "/datasets");
        System.setProperty("dw.securityConfiguration.localAuthentication.permissionConfig",tmpFolder +"/permissionConfig.json");
        System.setProperty("dw.securityConfiguration.localAuthentication.loginsFilePath", tmpFolder+"/logins.json");
        System.setProperty("dw.securityConfiguration.localAuthentication.usersFilePath", tmpFolder + "/users.json");
//        System.setProperty("dw.server.adminConnectors[0].port","8081");
//        System.setProperty("dw.server.adminConnectors","[]");
        System.setProperty("dw.baseUri","http://localhost:0");
        System.setProperty("dw.collectionFilters.elasticsearch.hostname", "" + elasticsearch.getContainerIpAddress());
        System.setProperty("dw.collectionFilters.elasticsearch.port",""+elasticsearch.getMappedPort(9200));
        //System.setProperty("dw.collectionFilters.elasticsearch.username","elastic");
        //System.setProperty("dw.collectionFilters.elasticsearch.password","changeme");
        System.setProperty("dw.webhooks.vreAdded","");
        System.setProperty("dw.webhooks.dataSetUpdated", "http://localhost:3000");
        System.setProperty("dw.databases.databaseLocation",tmpFolder+"/datasets");
        System.setProperty("dw.databaseConfiguration.databasePath", tmpFolder+"/neo4j");
        System.setProperty("dw.dataSet.dataStorage.rootDir",tmpFolder+"/datasets");


        try {
            application.run("server",
                    //"em/embedded/graphql/timbuctoo/src/main/resources/timbuctoo_evomaster.yaml"
                    "/timbuctoo_evomaster.yaml"
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        try {
            Thread.sleep(3_000);
        } catch (InterruptedException e) {
        }

        while (!application.getJettyServer().isStarted()) {
            try {
                Thread.sleep(1_000);
            } catch (InterruptedException e) {
            }
        }

        return "http://localhost:" + application.getJettyPort();
    }

    @Override
    public boolean isSutRunning() {
        if (application == null) {
            return false;
        }

        return application.getJettyServer().isRunning();
    }

    @Override
    public void stopSut() {
        if (application != null) {
            try {
                application.getJettyServer().stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        elasticsearch.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "nl.knaw.huygens.";
    }

    @Override
    public void resetStateOfSUT() {

        //TODO does elasticsearch need to be reset?

        try {
            //FIXME: this fails due to locks on Neo4j. need way to reset it
            //deleteDir(new File(tmpFolder));
            if(!Files.exists(Path.of(tmpFolder))) {
                Files.createDirectories(Path.of(tmpFolder));
            }
            Files.copy(getClass().getClassLoader().getResourceAsStream("users.json"), Path.of(tmpFolder,"users.json"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(getClass().getClassLoader().getResourceAsStream("logins.json"), Path.of(tmpFolder,"logins.json"), StandardCopyOption.REPLACE_EXISTING);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public ProblemInfo getProblemInfo() {
       return new GraphQlProblem("/v5/graphql");
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return List.of(AuthUtils.getForAuthorizationHeader("user", "u33707283d426f900d4d33707283d426f900d4d0d"));
    }



    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        boolean deleted = file.delete();
        if(! deleted){
            System.err.println("FAILED TO DELETE: " + file.getAbsolutePath());
        }
    }
}


class TimbuctooResource extends TimbuctooV4{

    @Override
    public void initialize(Bootstrap<TimbuctooConfiguration> bootstrap) {
        super.initialize(bootstrap);
        bootstrap.setConfigurationSourceProvider(new ResourceConfigurationSourceProvider());
    }

}