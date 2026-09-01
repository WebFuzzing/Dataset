package com.example.arimaabackend.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.example.arimaabackend.model.neo4j.GameTypeNode;

public interface GameTypeNeo4jRepository extends Neo4jRepository<GameTypeNode, Integer> {}

