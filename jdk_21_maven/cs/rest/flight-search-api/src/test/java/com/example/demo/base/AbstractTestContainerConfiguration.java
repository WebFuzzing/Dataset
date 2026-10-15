package com.example.demo.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Abstract base configuration class for tests using Docker containers.
 * This class provides setup for a Mongodb container
 * are created and indexed.
 * This class uses the Testcontainers library to manage the lifecycle of a Couchbase container
 * and ensures that the environment is properly prepared for integration tests.
 */
@Slf4j
@Testcontainers
public class AbstractTestContainerConfiguration {

    // Create a MongoDB container instance
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:latest")
            .withExposedPorts(27017);

    static {
        // Start the MongoDB container before all tests
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    public static void overrideProps(DynamicPropertyRegistry dynamicPropertyRegistry) {
        // Override MongoDB properties dynamically using the MongoDB container's details
        dynamicPropertyRegistry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        dynamicPropertyRegistry.add("spring.data.mongodb.host", mongoDBContainer::getHost);
        dynamicPropertyRegistry.add("spring.data.mongodb.port", () -> mongoDBContainer.getMappedPort(27017));
    }


}
