package com.example.arimaabackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.arimaabackend.migration.MigrationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MigrationProperties.class)
public class ArimaaBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArimaaBackendApplication.class, args);
    }

}
