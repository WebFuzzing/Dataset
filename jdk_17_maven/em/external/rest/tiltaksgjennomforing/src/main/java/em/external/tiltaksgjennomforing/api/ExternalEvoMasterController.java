package em.external.tiltaksgjennomforing.api;

import com.nimbusds.jose.JOSEObjectType;
import com.webfuzzing.commons.auth.LoginEndpoint;
import com.webfuzzing.commons.auth.TokenHandling;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.OAuth2Config;
import no.nav.security.mock.oauth2.token.RequestMapping;
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import com.github.dockerjava.api.model.Ulimit;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

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
        String jarLocation = "cs/rest/tiltaksgjennomforing/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/tiltaksgjennomforing-sut.jar";
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
        controller.setNeedsJdk17Options(true);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private final int timeoutSeconds;
    private final int sutPort;
    private  String jarLocation;
    private Connection sqlConnection;

    private List<DbSpecification> dbSpecification;


    private static final String POSTGRES_VERSION = "13.13";

    private static final String POSTGRES_PASSWORD = "password";

    private static final int POSTGRES_PORT = 5432;

    private static final java.util.function.Consumer<com.github.dockerjava.api.command.CreateContainerCmd> modifier =
            cmd -> cmd.getHostConfig().withUlimits(new Ulimit[]{new Ulimit("nofile", 65536L, 65536L)});

    private static final GenericContainer postgres = new GenericContainer("postgres:" + POSTGRES_VERSION)
            .withCreateContainerCmdModifier(modifier)
            .withEnv("POSTGRES_PASSWORD", POSTGRES_PASSWORD)
            .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust") //to allow all connections without a password
            .withEnv("POSTGRES_DB", "tiltaksgjennomforing")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
            .withExposedPorts(POSTGRES_PORT);

    private MockOAuth2Server oAuth2Server;

    private int oAuth2Port;

    private final String BESLUTTER_AD_GROUP = "99ea78dc-db77-44d0-b193-c5dc22f01e1d";


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
        this.oAuth2Port = sutPort + 1;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
        setJavaCommand(command);
    }


    @Override
    public String[] getInputParameters() {

        String wellKnownUrl = oAuth2Server.wellKnownUrl("aad").toString();
        String wellKnownUrlSystem = oAuth2Server.wellKnownUrl("system").toString();
        String wellKnownUrlTokenX = oAuth2Server.wellKnownUrl("tokenx").toString();

        return new String[]{
                "--server.port=" + sutPort,
                "--spring.profiles.active=dev-gcp-labs",
                "--spring.datasource.driverClassName=org.postgresql.Driver",
                "--spring.sql.init.platform=postgres",
                "--no.nav.security.jwt.issuer.aad.discoveryurl=" + wellKnownUrl,
                "--no.nav.security.jwt.issuer.aad.accepted_audience=aad",
                "--no.nav.security.jwt.issuer.system.discoveryurl=" + wellKnownUrlSystem,
                "--no.nav.security.jwt.issuer.system.accepted_audience=system",
                "--no.nav.security.jwt.issuer.tokenx.discoveryurl=" + wellKnownUrlTokenX,
                "--no.nav.security.jwt.issuer.tokenx.accepted_audience=tokenx",
                "--management.server.port=-1",
                "--server.ssl.enabled=false",
                "--spring.datasource.url=" + dbUrl(),
                "--spring.datasource.username=postgres",
                "--spring.datasource.password=" + POSTGRES_PASSWORD,
                "--sentry.logging.enabled=false",
                "--sentry.environment=local",
                "--logging.level.root=OFF",
                "--logging.config=classpath:logback-spring.xml",
                "--logging.level.org.springframework=INFO",
        };
    }

    public String[] getJVMParameters() {
        String wellKnownUrl = oAuth2Server.wellKnownUrl("aad").toString();
        String wellKnownUrlTokenX = oAuth2Server.wellKnownUrl("tokenx").toString();

        return new String[]{
                "-DAZURE_APP_WELL_KNOWN_URL=" + wellKnownUrl,
                "-DTOKEN_X_WELL_KNOWN_URL=" + wellKnownUrlTokenX,
                "-DVAULT_TOKEN=VAULT_TOKEN",
                "-DKAFKA_BROKERS=KAFKA_BROKERS",
                "-DKAFKA_TRUSTSTORE_PATH=KAFKA_TRUSTSTORE_PATH",
                "-DKAFKA_CREDSTORE_PASSWORD=KAFKA_CREDSTORE_PASSWORD",
                "-DKAFKA_KEYSTORE_PATH=KAFKA_KEYSTORE_PATH",
                "-DKAFKA_CREDSTORE_PASSWORD=KAFKA_CREDSTORE_PASSWORD",
                "-DKAFKA_SCHEMA_REGISTRY=KAFKA_SCHEMA_REGISTRY",
                "-DKAFKA_SCHEMA_REGISTRY_USER=KAFKA_SCHEMA_REGISTRY_USER",
                "-DKAFKA_SCHEMA_REGISTRY_PASSWORD=KAFKA_SCHEMA_REGISTRY_PASSWORD",
                "-DAZURE_APP_TENANT_ID=AZURE_APP_TENANT_ID",
                "-DAZURE_APP_CLIENT_ID=aad",
                "-DAZURE_APP_CLIENT_SECRET=secret",
                "-Dbeslutter.ad.gruppe=" + BESLUTTER_AD_GROUP
        };
    }

    private String dbUrl() {

        String host = postgres.getContainerIpAddress();
        int port = postgres.getMappedPort(5432);


        return "jdbc:postgresql://"+host+":"+port+"/tiltaksgjennomforing";
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
        return "Tomcat started on port";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    private RequestMapping getRequestMapping(String key, String value, String issuer, String subject, List<String> audience, String navIdent, String acrLevel, List<String> groups, String pid) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("groups", groups);
        claims.put("NAVident", navIdent);
        claims.put("sub", subject);
        claims.put("aud", audience);
        claims.put("roles", Arrays.asList("access_as_application"));
        claims.put("pid", pid);
        claims.put("tid", issuer);
        claims.put("azp", navIdent);
        claims.put("acr", acrLevel);
        claims.put("ver", "1.0");
        claims.put("nonce", "myNonce");

        RequestMapping rm = new RequestMapping(key, value, claims, JOSEObjectType.JWT.getType());

        return rm;
    }


    private OAuth2Config getOAuth2Config(){

        List<RequestMapping> mappings = Arrays.asList(
                getRequestMapping("NAVident", "Q987654", "aad","blablabla", Arrays.asList("aad"), "Q987654", "Level4", Arrays.asList(BESLUTTER_AD_GROUP), "aad")
        );

        List<RequestMapping> mappingsSystem = Arrays.asList(
                getRequestMapping("sub", "system", "system","system", Arrays.asList("system"), null, null, null, "system")
        );

        List<RequestMapping> mappingsTokenx = Arrays.asList(
                getRequestMapping("pid", "88888888888", "tokenx","tokenx", Arrays.asList("tokenx"), null, "Level3", null, "88888888888"),
                getRequestMapping("pid", "99999999999", "tokenx","tokenx", Arrays.asList("tokenx"), null, "Level4", null, "99999999999")
        );

        RequestMappingTokenCallback callback = new RequestMappingTokenCallback(
                "aad",
                mappings,
                360000
        );
        RequestMappingTokenCallback callbackSystem = new RequestMappingTokenCallback(
                "system",
                mappingsSystem,
                360000
        );

        RequestMappingTokenCallback callbackTokenx = new RequestMappingTokenCallback(
                "tokenx",
                mappingsTokenx,
                360000
        );

        Set<RequestMappingTokenCallback> callbacks = Set.of(
                callback,
                callbackSystem,
                callbackTokenx
        );

        return new OAuth2Config(
                true,
                null,
                null,
                false,
                new no.nav.security.mock.oauth2.token.OAuth2TokenProvider(),
                callbacks
        );
    }


    @Override
    public void preStart() {
        postgres.start();
        oAuth2Server = new  MockOAuth2Server(getOAuth2Config());
        oAuth2Server.start(oAuth2Port);
    }

    @Override
    public void postStart() {
        closeDataBaseConnection();

        try {
            sqlConnection = DriverManager.getConnection(dbUrl(), "postgres", POSTGRES_PASSWORD);
            dbSpecification = Arrays.asList(new DbSpecification(DatabaseType.POSTGRES,sqlConnection));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public void preStop() {
        closeDataBaseConnection();
    }

    @Override
    public void postStop() {
        postgres.stop();
        if(oAuth2Server!=null) oAuth2Server.shutdown();
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
        return "no.nav.tag.tiltaksgjennomforing.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/tiltaksgjennomforing-api/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    private AuthenticationDto getAuthenticationDto(String label, String keyValue, String oauth2Url){

        AuthenticationDto dto = new AuthenticationDto(label);
        LoginEndpoint x = new LoginEndpoint();
        dto.setLoginEndpointAuth(x);

        x.setExternalEndpointURL(oauth2Url);
        x.setPayloadRaw(keyValue+"&grant_type=client_credentials&code=foo&client_id=foo&client_secret=secret");
        x.setVerb(LoginEndpoint.HttpVerb.POST);
        x.setContentType("application/x-www-form-urlencoded");
        x.setExpectCookies(false);

        TokenHandling token = new TokenHandling();
        token.setSendTemplate("Bearer {token}");
        token.setSendName("Authorization");
        token.setSendIn(TokenHandling.SendIn.HEADER);
        token.setExtractSelector("/access_token");
        token.setExtractFrom(TokenHandling.ExtractFrom.BODY);
        x.setToken(token);

        return dto;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        String urlAad = oAuth2Server.baseUrl() + "aad/token";
        String urlSystem = oAuth2Server.baseUrl() + "system/token";
        String urlTokenX = oAuth2Server.baseUrl() + "tokenx/token";

        return Arrays.asList(
                getAuthenticationDto("aad","NAVident=Q987654", urlAad),
                getAuthenticationDto("system","sub=system", urlSystem),
                getAuthenticationDto("tokenxLevel3","pid=88888888888", urlTokenX),
                getAuthenticationDto("tokenxLevel4","pid=99999999999", urlTokenX)
        );
    }


    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }
}
