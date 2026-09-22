package com.example.gdprkv.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for audit log retention.
 * These values are bound from application.yml (audit.retention.*).
 * The defaults below serve as fallbacks if properties are missing from YAML.
 * To enable retention, set audit.retention.enabled=true in application.yml.
 */
@Component
@ConfigurationProperties(prefix = "audit.retention")
@Data
public class AuditRetentionProperties {

    private boolean enabled = false;
    private String schedule = "0 0 2 * * *";
    private int retentionDays = 730;
}
