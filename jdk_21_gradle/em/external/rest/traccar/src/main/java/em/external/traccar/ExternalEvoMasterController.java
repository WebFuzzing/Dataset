package em.external.traccar;

import org.evomaster.client.java.controller.AuthUtils;
import org.evomaster.client.java.controller.ExternalSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.h2.tools.Server;

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
        String jarLocation = "cs/rest/traccar/build/libs";
        if (args.length > 2) {
            jarLocation = args[2];
        }
        if (!jarLocation.endsWith(".jar")) {
            jarLocation += "/traccar-sut.jar";
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
        controller.setNeedsJdk17Options(true);
        InstrumentedSutStarter starter = new InstrumentedSutStarter(controller);

        starter.start();
    }

    private static final String INIT_SQL = String.join("\n",
            "INSERT INTO TC_SERVERS SELECT 1, FALSE, 0.0, 0.0, 0, NULL, NULL, NULL, FALSE, NULL, FALSE, NULL, FALSE, FALSE, NULL, NULL, FALSE, NULL, FALSE WHERE NOT EXISTS (SELECT ID FROM TC_SERVERS);",
            "INSERT INTO TC_USERS VALUES(1, 'admin', 'admin@traccar.invalid', 'PBKDF2WithHmacSHA256$200000$46dce9ed5c69fdabe7f50ccc27b62e012073cb4e5b649473', '966a83ce7accd9b20b3745a27336567ee0343cb1ffc71d50', FALSE, TRUE, NULL, 0.0, 0.0, 0, '{}', NULL, FALSE, NULL, -1, 0, FALSE, NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL, FALSE);",
            "INSERT INTO TC_USERS VALUES(2, 'user1', 'user1@traccar.invalid', 'PBKDF2WithHmacSHA256$200000$56390e6fc946644ed0e024f49cf68fbe7a4edad0d8276e17', '504a97670076b67950880318ff72c85217ad3d0301fd1936', FALSE, FALSE, NULL, 0.0, 0.0, 0, '{}', NULL, FALSE, NULL, 0, 0, FALSE, NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL, FALSE);",
            "INSERT INTO TC_USERS VALUES(3, 'user2', 'user2@traccar.invalid', 'PBKDF2WithHmacSHA256$200000$9668a06f91f3eaa62d5b299548a6cb458bb0ad6c10ca8396', '7a87a571c51a979d969eccbcc54fc84602c1a4fa4e0863b0', FALSE, FALSE, NULL, 0.0, 0.0, 0, '{}', NULL, FALSE, NULL, 0, 0, FALSE, NULL, FALSE, NULL, NULL, FALSE, FALSE, NULL, FALSE);"
    );

    private final int timeoutSeconds;
    private final int sutPort;
    private final int dbPort;
    private String jarLocation;
    private Server h2;
    private int runs = 0;
    private Path config;
    private Connection sqlConnection;
    private List<DbSpecification> dbSpecification;

    public ExternalEvoMasterController() {
        this(40100, "cs/rest/traccar/build/libs/traccar-sut.jar", 12345, 120, "java");
    }

    public ExternalEvoMasterController(String jarLocation) {
        this();
        this.jarLocation = jarLocation;
    }

    public ExternalEvoMasterController(
            int controllerPort, String jarLocation, int sutPort, int timeoutSeconds, String command) {

        if (jarLocation == null || jarLocation.isEmpty()) {
            throw new IllegalArgumentException("Missing jar location");
        }

        this.sutPort = sutPort;
        this.dbPort = sutPort + 1;
        this.jarLocation = jarLocation;
        this.timeoutSeconds = timeoutSeconds;
        setControllerPort(controllerPort);
        setJavaCommand(command);
    }

    @Override
    public String[] getInputParameters() {
        return new String[]{config.toString()};
    }

    @Override
    public String[] getJVMParameters() {
        return new String[]{};
    }

    private String dbUrl() {
        return "jdbc:h2:tcp://localhost:" + dbPort + "/mem:traccar_" + runs + ";DB_CLOSE_DELAY=-1";
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
        return "Started oejs.Server@";
    }

    @Override
    public long getMaxAwaitForInitializationInSeconds() {
        return timeoutSeconds;
    }

    @Override
    public void preStart() {
        try {
            h2 = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "" + dbPort, "-ifNotExists");
            h2.start();
            runs++;
            config = writeConfig();
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Path writeConfig() throws IOException {
        Path dir = Files.createTempDirectory("traccar");
        copyResources("schema", dir);
        copyResources("templates", dir);
        Files.createDirectories(dir.resolve("web"));
        String xml = String.join("\n",
                "<?xml version='1.0' encoding='UTF-8'?>",
                "<!DOCTYPE properties SYSTEM 'http://java.sun.com/dtd/properties.dtd'>",
                "<properties>",
                entry("web.port", "" + sutPort),
                entry("web.path", dir.resolve("web").toString()),
                entry("web.override", dir.resolve("override").toString()),
                entry("media.path", dir.resolve("media").toString()),
                entry("templates.root", dir.resolve("templates").toString()),
                entry("database.driver", "org.h2.Driver"),
                entry("database.url", dbUrl()),
                entry("database.user", "sa"),
                entry("database.password", ""),
                entry("database.changelog", dir.resolve("schema/changelog-master.xml").toString()),
                entry("protocols.enable", ""),
                entry("geocoder.enable", "false"),
                entry("server.statistics", ""),
                entry("logger.console", "true"),
                "</properties>");
        Path file = dir.resolve("traccar.xml");
        Files.writeString(file, xml, StandardCharsets.UTF_8);
        return file;
    }

    private static String entry(String key, String value) {
        return "    <entry key='" + key + "'>" + value + "</entry>";
    }

    // the folder is packed in this runner jar, or under build/resources when run from an IDE
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
    public void postStart() {
        closeDatabaseConnection();
        try {
            sqlConnection = DriverManager.getConnection(dbUrl(), "sa", "");
            dbSpecification = Arrays.asList(
                    new DbSpecification(DatabaseType.H2, sqlConnection).withInitSqlScript(INIT_SQL));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resetStateOfSUT() {
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
            h2 = null;
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "org.traccar.";
    }

    @Override
    public ProblemInfo getProblemInfo() {
        String schema = new Scanner(getClass().getResourceAsStream("/openapi.yaml"), "UTF-8").useDelimiter("\\A").next()
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
