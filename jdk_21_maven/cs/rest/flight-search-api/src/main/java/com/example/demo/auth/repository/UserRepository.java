package com.example.demo.auth.repository;

import com.example.demo.auth.model.entity.UserEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Repository interface for managing {@link UserEntity} objects in Couchbase.
 * This interface extends {@link MongoRepository} to provide CRUD operations for the {@link UserEntity}.
 * It also includes custom query methods to interact with the data store related to user entities.
 */
public interface UserRepository extends MongoRepository<UserEntity, String> {

    /**
     * Checks whether a {@link UserEntity} exists in the database with the specified email.
     * This method is used to verify if a user with the given email already exists in the database.
     *
     * @param email The email address to check for the existence of a user.
     * @return {@code true} if a user with the specified email exists, otherwise {@code false}.
     */
    boolean existsUserEntityByEmail(final String email);

    /**
     * Finds a {@link UserEntity} by its email address.
     * This method is used to retrieve a user entity based on the unique email address.
     *
     * @param email The email address of the user to search for.
     * @return An {@link Optional} containing the {@link UserEntity} if found, or {@link Optional#empty()} if not found.
     */
    Optional<UserEntity> findUserEntityByEmail(final String email);

}
