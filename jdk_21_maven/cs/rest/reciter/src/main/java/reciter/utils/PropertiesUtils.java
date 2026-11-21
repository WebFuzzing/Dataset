package reciter.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to read values from application.properties file.
 * This class is intended for use in non-Spring-managed beans.
 */
public class PropertiesUtils {

	// Static Properties object to hold loaded key-value pairs from the properties file
    private static final Properties properties = new Properties();

    // Static block to load the application.properties file when the class is first loaded
    static {
        try (InputStream input = PropertiesUtils.class.getClassLoader()
                .getResourceAsStream("application.properties")) {

        	 // Check if the file was found in the classpath
            if (input != null) {
                properties.load(input);  // Load the properties into the Properties object
            } else {
                throw new RuntimeException("application.properties file not found in classpath");
            }

        } catch (IOException e) {
        	 // Wrap and rethrow IOException as RuntimeException for easier handling
            throw new RuntimeException("Failed to load application.properties", e);
        }
    }
    
    /**
     * Gets the value of a property for the given key.
     *
     * @param key the key to look up
     * @return the value for the key, or null if not found
     */
    public static String get(String key) {
        return properties.getProperty(key);
    }
    
    /**
     * Gets the value of a property for the given key, with a default fallback.
     *
     * @param key          the key to look up
     * @param defaultValue the default value to return if key is not found
     * @return the value for the key, or the default value if not found
     */
    public static String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}
