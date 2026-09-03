package com.example.arimaabackend.repository.neo4j;

import org.springframework.data.neo4j.repository.Neo4jRepository;

import com.example.arimaabackend.model.neo4j.CountryNode;

public interface CountryNeo4jRepository extends Neo4jRepository<CountryNode, Integer> {}

