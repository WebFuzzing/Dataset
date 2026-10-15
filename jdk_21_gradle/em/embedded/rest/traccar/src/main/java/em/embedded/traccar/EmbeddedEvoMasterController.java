package em.embedded.traccar;

import com.google.inject.Injector;
import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.traccar.LifecycleObject;
import org.traccar.Main;
import org.traccar.web.WebServer;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
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

    private static final String INIT_SQL = String.join("\n",
            "INSERT INTO TC_SERVERS SELECT 1, FALSE, 0.0, 0.0, 0, NULL, NULL, NULL, FALSE, NULL, FALSE, NULL, FALSE, FALSE, NULL, NULL, FALSE, NULL, FALSE WHERE NOT EXISTS (SELECT ID FROM TC_SERVERS);",
            "INSERT INTO TC_USERS VALUES(1, 'admin', 'admin@traccar.invalid', 'PBKDF2WithHmacSHA256$200000$46dce9ed5c69fdabe7f50ccc27b62e012073cb4e5b649473', '966a83ce7accd9b20b3745a27336567ee0343cb1ffc71d50', FALSE, TRUE, NULL, 0.0, 0.0, 0, '{}', NULL, FALSE, NULL, -1, 0, FALSE, NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL, FALSE);",
            "INSERT INTO TC_USERS VALUES(2, 'user1', 'user1@traccar.invalid', 'PBKDF2WithHmacSHA256$200000$56390e6fc946644ed0e024f49cf68fbe7a4edad0d8276e17', '504a97670076b67950880318ff72c85217ad3d0301fd1936', FALSE, FALSE, NULL, 0.0, 0.0, 0, '{}', NULL, FALSE, NULL, 0, 0, FALSE, NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL, FALSE);",
            "INSERT INTO TC_USERS VALUES(3, 'user2', 'user2@traccar.invalid', 'PBKDF2WithHmacSHA256$200000$9668a06f91f3eaa62d5b299548a6cb458bb0ad6c10ca8396', '7a87a571c51a979d969eccbcc54fc84602c1a4fa4e0863b0', FALSE, FALSE, NULL, 0.0, 0.0, 0, '{}', NULL, FALSE, NULL, 0, 0, FALSE, NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL, FALSE);"
    );

    private static int runs = 0;

    private List<LifecycleObject> services;

    private WebServer webServer;

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

        String dbUrl = "jdbc:h2:mem:traccar_" + (++runs) + ";DB_CLOSE_DELAY=-1";

        services = Main.run(writeConfig(dbUrl).toString());
        if (services == null) {
            throw new IllegalStateException("Traccar failed to start");
        }
        webServer = services.stream()
                .filter(WebServer.class::isInstance)
                .map(WebServer.class::cast)
                .findFirst().orElseThrow();

        try {
            sqlConnection = DriverManager.getConnection(dbUrl, "sa", "");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        dbSpecification = Arrays.asList(
                new DbSpecification(DatabaseType.H2, sqlConnection).withInitSqlScript(INIT_SQL));

        return "http://localhost:" + webServer.getJettyPort();
    }

    private Path writeConfig(String dbUrl) {
        try {
            Path dir = Files.createTempDirectory("traccar");
            copyResources("schema", dir);
            copyResources("templates", dir);
            Files.createDirectories(dir.resolve("web"));
            String xml = String.join("\n",
                    "<?xml version='1.0' encoding='UTF-8'?>",
                    "<!DOCTYPE properties SYSTEM 'http://java.sun.com/dtd/properties.dtd'>",
                    "<properties>",
                    entry("web.port", "0"),
                    entry("web.path", dir.resolve("web").toString()),
                    entry("web.override", dir.resolve("override").toString()),
                    entry("media.path", dir.resolve("media").toString()),
                    entry("templates.root", dir.resolve("templates").toString()),
                    entry("database.driver", "org.h2.Driver"),
                    entry("database.url", dbUrl),
                    entry("database.user", "sa"),
                    entry("database.password", ""),
                    entry("database.changelog", dir.resolve("schema/changelog-master.xml").toString()),
                    entry("protocols.enable", ""),
                    entry("geocoder.enable", "false"),
                    entry("server.statistics", ""),
                    entry("logger.console", "true"),
                    "</properties>");
            Path config = dir.resolve("traccar.xml");
            Files.writeString(config, xml, StandardCharsets.UTF_8);
            return config;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String entry(String key, String value) {
        return "    <entry key='" + key + "'>" + value + "</entry>";
    }

    // the folders are resources of this driver, so the working directory does not matter
    private void copyResources(String folder, Path target) throws IOException {
        Path location;
        try {
            location = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (Files.isDirectory(location)) {
            Path resources = location.resolve("../../../resources/main").normalize();
            copyTree(resources.resolve(folder), target.resolve(folder));
        } else {
            try (FileSystem jar = FileSystems.newFileSystem(URI.create("jar:" + location.toUri()), Map.of())) {
                copyTree(jar.getPath("/" + folder), target.resolve(folder));
            }
        }
    }

    private static void copyTree(Path source, Path target) throws IOException {
        try (Stream<Path> files = Files.walk(source)) {
            for (Path p : (Iterable<Path>) files::iterator) {
                Path dest = target.resolve(source.relativize(p).toString());
                if (Files.isDirectory(p)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(p, dest);
                }
            }
        }
    }

    @Override
    public boolean isSutRunning() {
        return webServer != null && webServer.isRunning();
    }

    @Override
    public void stopSut() {
        if (sqlConnection != null) {
            try {
                sqlConnection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sqlConnection = null;
        }
        if (services != null) {
            try {
                for (LifecycleObject service : services) {
                    service.stop();
                }
                Injector injector = Main.getInjector();
                injector.getInstance(ExecutorService.class).shutdownNow();
                ((AutoCloseable) injector.getInstance(DataSource.class)).close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            services = null;
            webServer = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.traccar.";
    }

    @Override
    public void resetStateOfSUT() {
    }

    @Override
    public ProblemInfo getProblemInfo() {
        String schema = new Scanner(Main.class.getResourceAsStream("/openapi.yaml"), "UTF-8").useDelimiter("\\A").next()
                .replaceFirst("(?s)\nservers:.*?\nsecurity:", "\nservers:\n  - url: /api\nsecurity:");
        return new RestProblem(null, null, schema);
    }

    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return SutInfoDto.OutputFormat.JAVA_JUNIT_5;
    }

    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Arrays.asList(
                AuthUtils.getForBasic("admin", "admin@traccar.invalid", "admin123"),
                AuthUtils.getForBasic("user1", "user1@traccar.invalid", "user1123"),
                AuthUtils.getForBasic("user2", "user2@traccar.invalid", "user2123")
        );
    }

    @Override
    public List<DbSpecification> getDbSpecifications() {
        return dbSpecification;
    }

}
