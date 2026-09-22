package com.example.gdprkv.health;

import java.time.Instant;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private final BuildProperties buildProperties;
    private final String env;

    public HealthController(@Value("${app.env:local}") String env,
                            BuildProperties buildProperties) {
        this.env = env;
        this.buildProperties = buildProperties;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "ts", Instant.now().toString(),
                "env", env,
                "app", buildProperties != null ? buildProperties.getName() : "gdpr-kv",
                "version", buildProperties != null ? buildProperties.getVersion() : "dev"
        ));
    }
}
