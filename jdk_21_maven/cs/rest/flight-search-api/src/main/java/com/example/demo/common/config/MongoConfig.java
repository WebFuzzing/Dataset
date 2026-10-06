package com.example.demo.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

/**
 * Configuration class to enable auditing for MongoDB entities.
 */
@Configuration
@EnableMongoAuditing
public class MongoConfig {

}
