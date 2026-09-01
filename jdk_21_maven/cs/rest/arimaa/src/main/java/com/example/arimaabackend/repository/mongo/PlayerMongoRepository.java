package com.example.arimaabackend.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.arimaabackend.model.mongo.PlayerDocument;
import java.util.List;
import java.util.Optional;

public interface PlayerMongoRepository extends MongoRepository<PlayerDocument, Integer> {
    Optional<PlayerDocument> findByUser_Username(String username);
    List<PlayerDocument> findByCountry(String country);
}

