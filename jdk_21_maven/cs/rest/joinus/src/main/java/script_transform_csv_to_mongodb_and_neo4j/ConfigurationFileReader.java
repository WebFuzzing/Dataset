package script_transform_csv_to_mongodb_and_neo4j;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;


public class ConfigurationFileReader {

    private static final String pathOfApplicationProperties = "src/main/resources/application.properties";
    private static final Properties properties = new Properties();

    static {
        try {
            properties.load(new FileInputStream(pathOfApplicationProperties));
        } catch (IOException e) {
            throw new RuntimeException("Could not load the application.properties file");
        }
    }

    public ConfigurationFileReader() throws IOException {
        properties.load(new FileInputStream(pathOfApplicationProperties));
    }

    public static String getMongoUrl() {
        return checkAndGetProp("spring.mongodb.uri");
    }

    public static String getMongoDatabase() {
        return checkAndGetProp("spring.mongodb.database");
    }

    public static String getNeo4JDatabase() {
        return checkAndGetProp("spring.data.neo4j.database");
    }

    public static String getNeo4JURL() {
        return checkAndGetProp("spring.neo4j.uri");
    }

    public static String getNeo4JUsername() {
        return getProperty("spring.neo4j.authentication.username");
    }

    public static String getNeo4JPassword() {
        return getProperty("spring.neo4j.authentication.password");
    }

    public static String getProperty(String property) {
        return properties.getProperty(property);
    }

    //neo4j.username
    public static String checkAndGetProp(String property) {
        boolean exists = properties.getProperty(property) != null
                && !properties.getProperty(property).isEmpty()
                && !properties.getProperty(property).equalsIgnoreCase("");

        if (!exists) throw new RuntimeException("The property does not exist");

        return properties.getProperty(property);
    }

}
