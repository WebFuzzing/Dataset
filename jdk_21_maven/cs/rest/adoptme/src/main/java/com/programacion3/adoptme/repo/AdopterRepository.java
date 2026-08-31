package com.programacion3.adoptme.repo;

import com.programacion3.adoptme.domain.Adopter;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface AdopterRepository extends Neo4jRepository<Adopter, String> {}
