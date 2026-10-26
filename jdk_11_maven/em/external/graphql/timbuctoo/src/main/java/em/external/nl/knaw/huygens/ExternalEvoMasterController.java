package em.external.nl.knaw.huygens;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.GraphQlProblem;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
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
        String jarLocation = "cs/graphql/timbuctoo/timbuctoo-instancev4/target";
        if(args.length > 2){
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/timbuctoo-sut.jar";
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
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int timeoutSeconds;
    private final int sutPort;
    private  String jarLocation;
    private final String tmpFolder;
    private final String CONFIG_FILE = "timbuctoo_evomaster.yaml";

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer elasticsearch = new GenericContainer("elasticsearch:5.6.5")
            .withCreateContainerCmdModifier(modifier)
            .withExposedPorts(9200)
            //.withEnv("","")
            //.withTmpFs(Collections.singletonMap("", "rw"))
            ;

    public ExternalEvoMasterController(){
        this(40100, "../api/target", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(int controllerPort,
                                       String jarLocation,
                                       int sutPort,
                                       int timeoutSeconds,
                                       String command) {
        this.sutPort = sutPort;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
        setJavaCommand(command);

        String base = Paths.get(jarLocation).toAbsolutePath().getParent().normalize().toString();
        tmpFolder = base + "/temp/tmp_timbuctoo/temp_"+sutPort;
        createConfigurationFile();
    }

    /**
           Unfortunately, it seems like Dropwizard is buggy, and has
           problems with overriding params without a YML file :(
     */
    private void createConfigurationFile() {

        //save config to same folder of JAR file
        Path path = getConfigPath();

        try(InputStream is = this.getClass().getResourceAsStream("/"+ CONFIG_FILE )){
            Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    private Path getConfigPath(){
        return Paths.get(jarLocation)
                .toAbsolutePath()
                .getParent()
                .resolve(CONFIG_FILE)
                .normalize();
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{"server", getConfigPath().toAbsolutePath().toString()};
    }

    @Override
    public String[] getJVMParameters() {

        return new String[]{
                "-Ddw.server.applicationConnectors[0].port="+sutPort,
                "-Ddw.securityConfiguration.localAuthentication.authorizationsPath="+tmpFolder + "/datasets",
                "-Ddw.securityConfiguration.localAuthentication.permissionConfig="+tmpFolder +"/permissionConfig.json",
                "-Ddw.securityConfiguration.localAuthentication.loginsFilePath="+tmpFolder+"/logins.json",
                "-Ddw.securityConfiguration.localAuthentication.usersFilePath="+tmpFolder + "/users.json",
//                "-Ddw.server.adminConnectors[0].port="+(sutPort+1),
                "-Ddw.baseUri=http://localhost:0",
                "-Ddw.collectionFilters.elasticsearch.hostname="+elasticsearch.getContainerIpAddress(),
                "-Ddw.collectionFilters.elasticsearch.port="+elasticsearch.getMappedPort(9200),
                "-Ddw.collectionFilters.elasticsearch.username=elastic",
                "-Ddw.collectionFilters.elasticsearch.password=changeme",
                "-Ddw.webhooks.vreAdded=",
                "-Ddw.webhooks.dataSetUpdated=http://localhost:3000",
                "-Ddw.databases.databaseLocation="+tmpFolder+"/datasets",
                "-Ddw.databaseConfiguration.databasePath="+tmpFolder+"/neo4j",
                "-Ddw.dataSet.dataStorage.rootDir="+tmpFolder+"/datasets",
                "-Xmx8G"
        };
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:"+sutPort;
    }

    @Override
    public String getPathToExecutableJar() {
        return jarLocation;
    }

    @Override
    public String getLogMessageOfInitializedServer() {
        return "Started @";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        elasticsearch.start();
        resetStateOfSUT();
    }

    @Override
    public void postStart() {

        resetStateOfSUT();
    }

    @Override
    public void preStop() {

    }

    @Override
    public void postStop() {
        elasticsearch.stop();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "nl.knaw.huygens.";
    }

    @Override
    public void resetStateOfSUT() {
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



    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }
}
