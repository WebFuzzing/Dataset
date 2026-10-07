package com.example.demo.logging.repository;

import com.example.demo.logging.entity.LogEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Repository interface for {@link LogEntity}.
 * This interface extends {@link MongoRepository} and provides methods for interacting with
 * the Couchbase database for storing and retrieving {@link LogEntity} instances.
 * The repository will automatically inherit basic CRUD operations such as save, find, and delete
 * from {@link MongoRepository}.
 */
public interface LogRepository extends MongoRepository<LogEntity,String> {

}
