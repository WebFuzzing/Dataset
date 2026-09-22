package com.example.gdprkv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the purge sweeper job.
 * These values are bound from application.yml (purge.sweeper.*).
 * The defaults below serve as fallbacks if properties are missing from YAML.
 * To enable the sweeper, set purge.sweeper.enabled=true in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "purge.sweeper")
@Data
public class PurgeSweeperProperties {

    private boolean enabled = false;
    private String schedule = "0 */15 * * * *";  // Every 15 minutes by default
    private int lookbackHours = 24;  // How many hours worth of purge buckets to check
}
