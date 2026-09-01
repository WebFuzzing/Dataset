package com.example.arimaabackend.repository.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.arimaabackend.model.mongo.PuzzleDocument;

public interface PuzzleMongoRepository extends MongoRepository<PuzzleDocument, Integer> {}

