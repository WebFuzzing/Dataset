package em.external.familie.ba.sak;

import com.nimbusds.jose.JOSEObjectType;
import com.webfuzzing.commons.auth.LoginEndpoint;
import com.webfuzzing.commons.auth.TokenHandling;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.mock.oauth2.OAuth2Config;
import no.nav.security.mock.oauth2.token.RequestMapping;
import no.nav.security.mock.oauth2.token.RequestMappingTokenCallback;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbCleaner;
import org.evomaster.client.java.sql.SqlScriptRunner;
import org.evomaster.client.java.sql.SqlScriptRunnerCached;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
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
        String jarLocation = "cs/rest/familie-ba-sak/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/familie-ba-sak-sut.jar";
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
            .withEnv("POSTGRES_DB", "familiebasak")
            .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))
            .withExposedPorts(POSTGRES_PORT);

    private MockOAuth2Server oAuth2Server;

    private int oAuth2Port;

    private final String ISSUER_ID = "azuread";

    private final String DEFAULT_AUDIENCE = "some-audience";

    private final String PROSESSERING_ROLLE = "928636f4-fd0d-4149-978e-a6fb68bb19de";

    private final String TOKEN_PARAM = "name";

    private static final String A0 = "TaskRunner";
    private static final String A1 = "Veileder";
    private static final String A2 = "Saksbehandler";
    private static final String A3 = "Beslutter";
    private static final String A4 = "Forvalter";
    private static final String A5 = "Kode6";
    private static final String A6 = "Kode7";
    private static final String A7 = "System";

    private static final String veileder =  "93a26831-9866-4410-927b-74ff51a9107c";
    private static final String saksbehandler = "d21e00a4-969d-4b28-8782-dc818abfae65";
    private static final String beslutter = "9449c153-5a1e-44a7-84c6-7cc7a8867233";
    private static final String forvalter = "c62e908a-cf20-4ad0-b7b3-3ff6ca4bf38b";
    private static final String kode6 = "5ef775f2-61f8-4283-bf3d-8d03f428aa14";
    private static final String kode7 = "ea930b6b-9397-44d9-b9e6-f4cf527a632a";



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

        String wellKnownUrl = oAuth2Server.wellKnownUrl(ISSUER_ID).toString();

        return new String[]{
                "--server.port=" + sutPort,
                "--spring.profiles.active=dev",
                "--management.server.port=-1",
                "--server.ssl.enabled=false",
                "--spring.datasource.url=" + dbUrl(),
                "--spring.datasource.username=postgres",
                "--spring.datasource.password=" + POSTGRES_PASSWORD,
                "--sentry.logging.enabled=false",
                "--sentry.environment=local",
                //TODO check when dealing with Kafka
                "--funksjonsbrytere.kafka.producer.enabled=false",
                "--funksjonsbrytere.enabled=false",
                "--logging.level.root=OFF",
                "--logging.config=classpath:logback-spring.xml",
                "--logging.level.org.springframework=INFO",
                "--no.nav.security.jwt.issuer.azuread.discoveryurl="+wellKnownUrl,
                "--prosessering.rolle=" + PROSESSERING_ROLLE,
                "--FAMILIE_EF_SAK_API_URL=http://fake-familie-ef-sak/api",
                "--FAMILIE_KLAGE_URL=http://fake-familie-klage",
                "--FAMILIE_BREV_API_URL=http://fake-familie-brev",
                "--FAMILIE_BA_INFOTRYGD_FEED_API_URL=http://fake-familie-ba-infotrygd-feed/api",
                "--FAMILIE_BA_INFOTRYGD_API_URL=http://fake-familie-ba-infotrygd",
                "--FAMILIE_TILBAKE_API_URL=http://fake-familie-tilbake/api",
                "--PDL_URL=http://fake-pdl-api.default",
                "--FAMILIE_INTEGRASJONER_API_URL=http://fake-familie-integrasjoner/api",
                "--FAMILIE_OPPDRAG_API_URL=http://fake-familie-oppdrag/api",
                "--SANITY_FAMILIE_API_URL=http://fake-xsrv1mh6.apicdn.sanity.io/v2021-06-07/data/query/ba-brev",
                "--ECB_API_URL=http://fake-data-api.ecb.europa.eu/service/data/EXR/",
                "--rolle.veileder=" + veileder,
                "--rolle.saksbehandler=" + saksbehandler,
                "--rolle.beslutter=" + beslutter,
                "--rolle.forvalter=" + forvalter,
                "--rolle.kode6=" + kode6,
                "--rolle.kode7=" + kode7
        };
    }

    public String[] getJVMParameters() {

        return new String[]{
                "-DAZUREAD_TOKEN_ENDPOINT_URL=http://fake-azure-token-endpoint.no:8080",
                "-DAZURE_OPENID_CONFIG_TOKEN_ENDPOINT=bar",
                "-DAZURE_APP_CLIENT_ID=bar",
                "-DNAIS_APP_NAME=bar",
                "-DUNLEASH_SERVER_API_URL=http://fake-unleash-server-api.no:8080",
                "-DUNLEASH_SERVER_API_TOKEN=bar",
                "-DBA_SAK_CLIENT_ID="+DEFAULT_AUDIENCE
        };
    }

    private String dbUrl() {

        String host = postgres.getContainerIpAddress();
        int port = postgres.getMappedPort(5432);


        return "jdbc:postgresql://"+host+":"+port+"/familiebasak";
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
        return "Jetty started on port";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
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
        return "no.nav.familie.ba.sak.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }


    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {

        String url = oAuth2Server.baseUrl() + ISSUER_ID + "/token";

        return Arrays.asList(
                getAuthenticationDto(A0,url),
                getAuthenticationDto(A1,url),
                getAuthenticationDto(A2,url),
                getAuthenticationDto(A3,url),
                getAuthenticationDto(A4,url),
                getAuthenticationDto(A5,url),
                getAuthenticationDto(A6,url),
                getAuthenticationDto(A7,url)
        );
    }

    private RequestMapping getRequestMapping(String label, List<String> groups, String id, String name) {
        Map<String,Object> claims = new HashMap<>();
        claims.put("groups",groups);
        claims.put("name",name);
        claims.put("NAVident", id);
        claims.put("sub","subject");
        claims.put("aud","some-audience");
        claims.put("tid",ISSUER_ID);
        claims.put("azp",id);

        RequestMapping rm = new RequestMapping(TOKEN_PARAM,label,claims, JOSEObjectType.JWT.getType());

        return rm;
    }

    private OAuth2Config getOAuth2Config(){

        List<RequestMapping> mappings = Arrays.asList( getRequestMapping(A0, Arrays.asList(PROSESSERING_ROLLE),"Z0042", "Task Runner"),
                getRequestMapping(A1, Arrays.asList(veileder),"Z0000", "Mock McMockface"),
                getRequestMapping(A2, Arrays.asList(saksbehandler),"Z0001", "Foo Bar"),
                getRequestMapping(A3, Arrays.asList(beslutter),"Z0002", "John Smith"),
                getRequestMapping(A4, Arrays.asList(forvalter),"Z0003", "Mario Rossi"),
                getRequestMapping(A5, Arrays.asList(kode6),"Z0004", "Kode Six"),
                getRequestMapping(A6, Arrays.asList(kode7),"Z0005", "Kode Seven"),
                getRequestMapping(A7, Arrays.asList(),"VL", "The System")
        );

        RequestMappingTokenCallback callback = new RequestMappingTokenCallback(
                ISSUER_ID,
                mappings,
                360000
        );

        Set<RequestMappingTokenCallback> callbacks = Set.of(
                callback
        );

        OAuth2Config config = new OAuth2Config(
                true,
                null,
                null,
                false,
                new no.nav.security.mock.oauth2.token.OAuth2TokenProvider(),
                callbacks
        );

        return config;
    }

    private AuthenticationDto getAuthenticationDto(String label, String oauth2Url){

        AuthenticationDto dto = new AuthenticationDto(label);
        LoginEndpoint x = new LoginEndpoint();
        dto.setLoginEndpointAuth(x);

        x.setExternalEndpointURL(oauth2Url);
        x.setPayloadRaw(TOKEN_PARAM+"="+label+"&grant_type=client_credentials&code=foo&client_id=foo&client_secret=secret");
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
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }
}
