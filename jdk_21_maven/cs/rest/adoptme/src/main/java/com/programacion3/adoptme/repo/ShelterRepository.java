package com.programacion3.adoptme.repo;

import com.programacion3.adoptme.domain.Shelter;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface ShelterRepository extends Neo4jRepository<Shelter, String> {}
