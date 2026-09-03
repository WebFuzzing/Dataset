package com.example.arimaabackend.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.arimaabackend.model.mongo.MatchDocument;
import java.util.List;

public interface MatchMongoRepository extends MongoRepository<MatchDocument, Integer> {
    @Query("{ 'players.playerId': ?0 }")
    List<MatchDocument> findByPlayerId(Integer playerId);
}
