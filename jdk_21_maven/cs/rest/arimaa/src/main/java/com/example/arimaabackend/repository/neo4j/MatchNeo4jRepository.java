package com.example.arimaabackend.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.example.arimaabackend.model.neo4j.MatchNode;

public interface MatchNeo4jRepository extends Neo4jRepository<MatchNode, Integer> {}

