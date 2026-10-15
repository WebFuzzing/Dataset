package com.example.demo.flight.repository;

import com.example.demo.flight.model.entity.AirportEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for managing {@link AirportEntity} entities in the MongoDB database.
 * Provides CRUD operations and custom query methods for handling {@link AirportEntity} records.
 */
public interface AirportRepository extends MongoRepository<AirportEntity,String> {

    /**
     * Checks if a task with the specified name already exists in the database.
     *
     * @param name the name of the task to check for existence.
     * @return {@code true} if a task with the specified name exists, {@code false} otherwise.
     */
    boolean existsByName(String name);

}
