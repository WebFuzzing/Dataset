package em.embedded.commafeed;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.sql.DbSpecification;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import io.quarkus.runtime.Quarkus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;


public class EmbeddedEvoMasterController extends EmbeddedSutController {

    static {
        // Both must be set before anything touches JUL or Quarkus config.
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        // Placeholder normally supplied by the Quarkus platform descriptor at build time.
        System.setProperty("platform.quarkus.native.builder-image", "unused-by-emb");
    }

    public static void main(String[] args) {

        int port = 40100;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        EmbeddedEvoMasterController controller = new EmbeddedEvoMasterController(port);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

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

    private static final String DB_URL = "jdbc:h2:mem:commafeed_embedded;DB_CLOSE_DELAY=-1";

    private int sutPort;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;


    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }

    @Override
    public String startSut() {

        sutPort = findFreePort();

        // Essential: without this, Quarkus's isolated classloader gets its own ExecutionTracer and
        // the instrumented SUT reports to a counter the driver never reads, giving 0% coverage.
        System.setProperty("quarkus.class-loading.parent-first-artifacts",
                "org.evomaster:evomaster-client-java-instrumentation,"
                        + "org.evomaster:evomaster-client-java-instrumentation-shared,"
                        + "org.evomaster:evomaster-client-java-distance-heuristics,"
                        + "org.evomaster:evomaster-client-java-util");

        // Quarkus treats this driver module as the application and commafeed-server as a plain
        // library, so the SUT's CDI beans are not discovered unless it is indexed explicitly.
        System.setProperty("quarkus.index-dependency.commafeed.group-id", "com.commafeed");
        System.setProperty("quarkus.index-dependency.commafeed.artifact-id", "commafeed-server");

        // Quarkus reads config from system properties, not program arguments.
        // Same JVM as the SUT, so no H2 TCP server is needed: the driver opens the same URL.
        System.setProperty("quarkus.http.port", String.valueOf(sutPort));
        System.setProperty("quarkus.datasource.db-kind", "h2");
        System.setProperty("quarkus.datasource.jdbc.url", DB_URL);
        System.setProperty("quarkus.datasource.username", "sa");
        System.setProperty("quarkus.datasource.password", "sa");

        // Quarkus.run() blocks until shutdown, so it needs its own thread.
        Thread thread = new Thread(() -> Quarkus.run(), "quarkus-sut");
        thread.setDaemon(true);
        thread.start();

        waitUntilListening();

        try {
            Class.forName("org.h2.Driver");
            sqlConnection = DriverManager.getConnection(DB_URL, "sa", "sa");
            dbSpecification = Arrays.asList(
                    new DbSpecification(DatabaseType.H2, sqlConnection).withInitSqlScript(INIT_SQL));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return "http://localhost:" + getSutPort();
    }

    protected int getSutPort() {
        return sutPort;
    }

    @Override
    public boolean isSutRunning() {
        return isListening();
    }

    @Override
    public void stopSut() {
        Quarkus.blockingExit();
        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.commafeed.";
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

        return Arrays.asList(
                AuthUtils.getForBasic("admin", "admin", "admin123"),
                AuthUtils.getForBasic("user1", "user1", "user1123"),
                AuthUtils.getForBasic("user2", "user2", "user2123"));
    }



    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                "http://localhost:" + getSutPort() + "/openapi?format=json",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }


    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Cannot find a free port for the SUT", e);
        }
    }

    /*
        Liveness of the thread we started is NOT a usable signal: Quarkus.run() hands the
        application over to its own "Quarkus Main Thread" and returns immediately. Poll the port.
     */
    private void waitUntilListening() {

        long deadline = System.currentTimeMillis() + 180_000;

        while (System.currentTimeMillis() < deadline) {
            if (isListening()) {
                return;
            }
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }

        throw new IllegalStateException("The SUT did not start listening on port " + sutPort);
    }

    private boolean isListening() {
        if (sutPort <= 0) {
            return false;
        }
        try (Socket probe = new Socket("localhost", sutPort)) {
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
