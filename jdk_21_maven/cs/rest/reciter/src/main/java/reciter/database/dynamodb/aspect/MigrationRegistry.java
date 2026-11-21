package reciter.database.dynamodb.aspect;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
@ConfigurationProperties(prefix = "dynamodb")
public class MigrationRegistry {

	// This maps the "migrations" section
    // The key will be "analysis" and the value will be a MigrationSettings object
    private Map<String, MigrationSettings> migrations = new HashMap<>();

    public Map<String, MigrationSettings> getMigrations() {
        return migrations;
    }

    public void setMigrations(Map<String, MigrationSettings> migrations) {
        this.migrations = migrations;
    }

    public int getTargetVersion(String tableName) {
        if (migrations != null && migrations.containsKey(tableName)) {
            return migrations.get(tableName).getSchema().getVersion();
        }
        return 1; // Default fallback
    }

    /**
     * Inner class to match the nested structure: .schema.version
     */
    public static class MigrationSettings {
        private SchemaSettings schema = new SchemaSettings();

        public SchemaSettings getSchema() { return schema; }
        public void setSchema(SchemaSettings schema) { this.schema = schema; }

        public static class SchemaSettings {
            private int version;
            public int getVersion() { return version; }
            public void setVersion(int version) { this.version = version; }
        }
    }
}
