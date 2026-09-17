package em.embedded.komga;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.gotson.komga.Application;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Class used to start/stop the SUT.
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

    private static final String[][] SEEDED_USERS = {
            {"wfd_admin1@wfd.invalid", "Wfd-Admin-Pass1", "[\"ADMIN\",\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"},
            {"wfd_admin2@wfd.invalid", "Wfd-Admin-Pass2", "[\"ADMIN\",\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"},
            {"wfd_user1@wfd.invalid", "Wfd-User-Pass1", "[\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"},
            {"wfd_user2@wfd.invalid", "Wfd-User-Pass2", "[\"FILE_DOWNLOAD\",\"PAGE_STREAMING\",\"KOBO_SYNC\",\"KOREADER_SYNC\"]"}
    };

    private static final Path CONFIG_DIR = Paths.get("tmp", "komga", "embedded").toAbsolutePath();

    /** live database file -> snapshot taken right after seeding */
    private static final List<String> DATABASES = List.of("database.sqlite", "tasks.sqlite");

    private ConfigurableApplicationContext ctx;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public EmbeddedEvoMasterController() {
        this(0);
    }

    public EmbeddedEvoMasterController(int port) {
        setControllerPort(port);
    }


    @Override
    public String startSut() {

        // a fresh config dir means a fresh SQLite database, with no user in it
        deleteRecursively(CONFIG_DIR);

        // same as what Komga's own main() does before starting Spring
        System.setProperty("org.jooq.no-logo", "true");
        System.setProperty("org.jooq.no-tips", "true");

        ctx = SpringApplication.run(Application.class,
                new String[]{"--server.port=0",
                        "--komga.config-dir=" + CONFIG_DIR,
                        // Logback holds this file open, it must sit outside the wiped config dir
                        "--logging.file.name=" + CONFIG_DIR.getParent().resolve("embedded.log")
                });

        seedUsers();
        eachDatabase("backup to");

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
        ctx.close();
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.gotson.komga.";
    }

    @Override
    public void resetStateOfSUT() {
        eachDatabase("restore from");
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Stream.of(SEEDED_USERS)
                .map(u -> AuthUtils.getForBasic(u[0], u[0], u[1]))
                .collect(Collectors.toList());
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


    private void seedUsers() {
        String baseUrl = "http://localhost:" + getSutPort();

        send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/claim"))
                .header("X-Komga-Email", SEEDED_USERS[0][0])
                .header("X-Komga-Password", SEEDED_USERS[0][1])
                .POST(HttpRequest.BodyPublishers.noBody()));

        String admin = "Basic " + Base64.getEncoder().encodeToString(
                (SEEDED_USERS[0][0] + ":" + SEEDED_USERS[0][1]).getBytes());

        for (int i = 1; i < SEEDED_USERS.length; i++) {
            String body = "{\"email\":\"" + SEEDED_USERS[i][0] + "\",\"password\":\"" + SEEDED_USERS[i][1]
                    + "\",\"roles\":" + SEEDED_USERS[i][2] + "}";
            send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v2/users"))
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
            Path live = CONFIG_DIR.resolve(db);
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
