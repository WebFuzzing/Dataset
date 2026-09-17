package em.external.komga;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
        String jarLocation = "cs/rest/komga/komga/build/libs";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if(! jarLocation.endsWith(".jar")) {
            jarLocation += "/komga-sut.jar";
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

    private static final String[][] SEEDED_USERS = {
            {"wfd_admin1@wfd.invalid", "Wfd-Admin-Pass1", "[\"ADMIN\",\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"},
            {"wfd_admin2@wfd.invalid", "Wfd-Admin-Pass2", "[\"ADMIN\",\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"},
            {"wfd_user1@wfd.invalid", "Wfd-User-Pass1", "[\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"},
            {"wfd_user2@wfd.invalid", "Wfd-User-Pass2", "[\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"}
    };

    private final int timeoutSeconds;
    private final int sutPort;
    private String jarLocation;

    private final Path configDir;

    /** live database file -> snapshot taken right after seeding */
    private static final List<String> DATABASES = List.of("database.sqlite", "tasks.sqlite");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();


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
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        this.configDir = Paths.get("tmp", "komga", "p" + sutPort).toAbsolutePath();
        setControllerPort(controllerPort);
        setJavaCommand(command);
    }


    @Override
    public String[] getInputParameters() {
        return new String[]{
                "--server.port=" + sutPort,
                "--komga.config-dir=" + configDir
        };
    }

    public String[] getJVMParameters() {
        return new String[]{};
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
        return "Started ApplicationKt in";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        // a fresh config dir means a fresh SQLite database, with no user in it
        deleteRecursively(configDir);
    }

    @Override
    public void postStart() {
        seedUsers();
        eachDatabase("backup to");
    }

    @Override
    public void resetStateOfSUT() {
        eachDatabase("restore from");
    }

    @Override
    public void preStop() {
    }

    @Override
    public void postStop() {
    }


    @Override
    public String getPackagePrefixesToCover() {
        return "org.gotson.komga.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem(
                getBaseURL() + "/v3/api-docs",
                null
        );
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Stream.of(SEEDED_USERS)
                .map(u -> AuthUtils.getForBasic(u[0], u[0], u[1]))
                .collect(Collectors.toList());
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }


    private void seedUsers() {
        send(HttpRequest.newBuilder(URI.create(getBaseURL() + "/api/v1/claim"))
                .header("X-Komga-Email", SEEDED_USERS[0][0])
                .header("X-Komga-Password", SEEDED_USERS[0][1])
                .POST(HttpRequest.BodyPublishers.noBody()));

        String admin = "Basic " + Base64.getEncoder().encodeToString(
                (SEEDED_USERS[0][0] + ":" + SEEDED_USERS[0][1]).getBytes());

        for (int i = 1; i < SEEDED_USERS.length; i++) {
            String body = "{\"email\":\"" + SEEDED_USERS[i][0] + "\",\"password\":\"" + SEEDED_USERS[i][1]
                    + "\",\"roles\":" + SEEDED_USERS[i][2] + "}";
            send(HttpRequest.newBuilder(URI.create(getBaseURL() + "/api/v2/users"))
                    .header("Authorization", admin)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)));
        }
    }

    private void send(HttpRequest.Builder builder) {
        try {
            HttpResponse<String> response = http.send(builder.timeout(Duration.ofSeconds(30)).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                throw new IllegalStateException("Failed to seed users: " + response.statusCode() + " " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /** sqlite-jdbc extension over SQLite's Online Backup API: safe while the SUT is connected */
    private void eachDatabase(String command) {
        for (String db : DATABASES) {
            Path live = configDir.resolve(db);
            try (Connection connection = DriverManager.getConnection(
                         "jdbc:sqlite:" + live + "?busy_timeout=30000");
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate(command + " '" + live + ".seed'");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
