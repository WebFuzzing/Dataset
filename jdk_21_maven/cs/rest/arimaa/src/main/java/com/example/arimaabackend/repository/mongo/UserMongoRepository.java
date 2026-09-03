package com.example.arimaabackend.repository.mongo;

import com.example.arimaabackend.model.mongo.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<UserDocument, Long> {
    Optional<UserDocument> findByUsername(String username);
}

