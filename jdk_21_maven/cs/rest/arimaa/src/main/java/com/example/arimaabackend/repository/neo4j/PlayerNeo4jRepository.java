package com.example.arimaabackend.repository.neo4j;

import java.util.List;
import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import com.example.arimaabackend.model.neo4j.PlayerNode;

public interface PlayerNeo4jRepository extends Neo4jRepository<PlayerNode, Integer> {

    @Query("""
            MATCH (p:Player)-[:HAS_USER]->(u:User {username: $username})
            RETURN p
            """)
    Optional<PlayerNode> findByUsername(String username);

    @Query("""
            MATCH (p:Player)-[:IN_COUNTRY]->(c:Country {name: $countryName})
            RETURN p
            """)
    List<PlayerNode> findByCountryName(String countryName);
}
