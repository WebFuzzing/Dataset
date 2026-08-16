package em.external.commafeed;


import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.h2.tools.Server;

public class ExternalEvoMasterController extends ExternalSutController {

    private static final int DEFAULT_CONTROLLER_PORT = 40100;

    private static final int DEFAULT_SUT_PORT = 12345;


    public static void main(String[] args) {

        int controllerPort = DEFAULT_CONTROLLER_PORT;
        if (args.length > 0) {
            controllerPort = Integer.parseInt(args[0]);
        }
        int sutPort = DEFAULT_SUT_PORT;
        if (args.length > 1) {
            sutPort = Integer.parseInt(args[1]);
        }
        String jarLocation = "cs/rest/commafeed/commafeed-server/target";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/commafeed-sut.jar";
        }

        int timeoutSeconds = 120;
        if (args.length > 3) {
            timeoutSeconds = Integer.parseInt(args[3]);
        }

        String command = "java";
        if (args.length > 4) {
            command = args[4];
        }

        ExternalEvoMasterController controller =
                new ExternalEvoMasterController(controllerPort, jarLocation, sutPort, timeoutSeconds, command);
        /*
            Required on JDK 9+: the EvoMaster instrumentation agent reflects into java.base,
            which the module system denies unless --add-opens is passed to the SUT's JVM.
            Without this, the SUT dies at startup with InaccessibleObjectException on
            java.lang.ClassLoader.findLoadedClass.
         */
        controller.setNeedsJdk17Options(true);

        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }


    private final int timeoutSeconds;

    private final int sutPort;
    private final int dbPort;
    private Server h2;

    private String jarLocation;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

    public ExternalEvoMasterController() {
        this(DEFAULT_CONTROLLER_PORT, "../target/commafeed-sut.jar", DEFAULT_SUT_PORT, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command) {
        this.sutPort = sutPort;
        this.dbPort = sutPort + 1;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;

        setControllerPort(controllerPort);
        setJavaCommand(command);
    }

    /*
        Unlike Spring Boot, Quarkus does not read "--key=value" program arguments as
        configuration. Everything has to be a "-D" system property, which means it belongs
        in getJVMParameters() and not here.
     */
    @Override
    public String[] getInputParameters() {
        return new String[]{};
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{
                "-Dquarkus.http.port=" + sutPort,
                "-Dquarkus.datasource.db-kind=h2",
                "-Dquarkus.datasource.jdbc.url=" + dbUrl(),
                "-Dquarkus.datasource.username=sa",
                "-Dquarkus.datasource.password=sa"
                /*
                    No account-related flag is needed here: the SUT seeds admin/user1/user2 on
                    first startup, see the MODIFIED block in CommaFeedApplication.java.
                 */
        };
    }

    private String dbUrl() {
        return "jdbc:h2:tcp://localhost:" + dbPort + "/mem:commafeed_" + dbPort + ";DB_CLOSE_DELAY=-1";
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:" + sutPort;
    }

    @Override
    public String getPathToExecutableJar() {
        return jarLocation;
    }

    /*
        Quarkus prints eg:
        "commafeed-server 7.3.0 on JVM (powered by Quarkus 3.38.2) started in 7.217s.
         Listening on: http://0.0.0.0:8080"
     */
    @Override
    public String getLogMessageOfInitializedServer() {
        return "started in";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    /*
        "-ifNotExists" is required: since H2 2.x, a TCP server refuses to create a database
        requested by a remote client ("Database ... not found, either pre-create it or allow
        remote database creation"). The SUT is such a remote client here.
     */
    @Override
    public void preStart() {
        try {
            h2 = Server.createTcpServer(
                    "-tcp", "-tcpAllowOthers", "-tcpPort", "" + dbPort, "-ifNotExists");
            h2.start();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*
        The SUT seeds admin/user1/user2 at startup, but EvoMaster's "smart DB clean" deletes
        the rows of every table a test touched. Once a test hits USERS, those accounts are
        gone and all authenticated requests start answering 401.
        So the same accounts are re-inserted before each test through this init script.

        PASSWORD is a salted hash, so the values cannot be written by hand: they were read
        from a running instance seeded by CommaFeedApplication. The hash is deterministic
        for a given (password, salt) pair, so these fixed rows keep matching the plain-text
        credentials declared in getInfoForAuthentication().

        MERGE, not INSERT: the SUT has already seeded these same ids at startup (with a
        randomly generated salt), so a plain INSERT would fail on the primary key. MERGE also
        pins the salt/hash to the known values, whatever the SUT generated for this run.
     */
    private static final String INIT_SQL = String.join("\n",
            "MERGE INTO USERS (ID, NAME, EMAIL, PASSWORD, SALT, DISABLED, CREATED) KEY(ID) VALUES",
            " (2, 'admin', 'admin@commafeed.invalid',",
            "  X'a78462049d55cd592d6d90f4d37bc56b8ad01eb5', X'920e32b33fa227f7',",
            "  FALSE, TIMESTAMP '2026-01-01 00:00:00');",
            "MERGE INTO USERS (ID, NAME, EMAIL, PASSWORD, SALT, DISABLED, CREATED) KEY(ID) VALUES",
            " (3, 'user1', 'user1@commafeed.invalid',",
            "  X'442a8b9a5c30dae41ed7136b756b13f35b7441eb', X'08a84a3a4b898356',",
            "  FALSE, TIMESTAMP '2026-01-01 00:00:00');",
            "MERGE INTO USERS (ID, NAME, EMAIL, PASSWORD, SALT, DISABLED, CREATED) KEY(ID) VALUES",
            " (4, 'user2', 'user2@commafeed.invalid',",
            "  X'e6fd4191e3298747a115540a586a9814c5683f26', X'421d645688f9404a',",
            "  FALSE, TIMESTAMP '2026-01-01 00:00:00');",
            "MERGE INTO USERROLES (ID, ROLENAME, USER_ID) KEY(ID) VALUES (2, 'ADMIN', 2);",
            "MERGE INTO USERROLES (ID, ROLENAME, USER_ID) KEY(ID) VALUES (3, 'USER', 2);",
            "MERGE INTO USERROLES (ID, ROLENAME, USER_ID) KEY(ID) VALUES (4, 'USER', 3);",
            "MERGE INTO USERROLES (ID, ROLENAME, USER_ID) KEY(ID) VALUES (5, 'USER', 4);"
    );

    @Override
    public void postStart() {
        closeDatabaseConnection();

        try {
            Class.forName("org.h2.Driver");
            sqlConnection = DriverManager.getConnection(dbUrl(), "sa", "sa");
            dbSpecification = Arrays.asList(
                    new DbSpecification(DatabaseType.H2, sqlConnection).withInitSqlScript(INIT_SQL));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void preStop() {
        closeDatabaseConnection();
    }

    private void closeDatabaseConnection() {
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
    public void postStop() {
        if (h2 != null) {
            h2.stop();
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.commafeed.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + sutPort + "/openapi?format=json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    /*
        The three accounts the SUT seeds at startup. "user1" and "user2" deliberately share
        the same role, so that broken access control between users is detectable.
     */
    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForBasic("admin", "admin", "admin123"),
                AuthUtils.getForBasic("user1", "user1", "user1123"),
                AuthUtils.getForBasic("user2", "user2", "user2123")
        );
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

}
