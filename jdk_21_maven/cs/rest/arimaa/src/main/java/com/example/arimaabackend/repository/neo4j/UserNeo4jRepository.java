package com.example.arimaabackend.repository.neo4j;

import com.example.arimaabackend.model.neo4j.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface UserNeo4jRepository extends Neo4jRepository<UserNode, Long> {}

