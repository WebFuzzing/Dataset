package com.programacion3.adoptme.repo;

import com.programacion3.adoptme.domain.Dog;
import org.springframework.data.neo4j.repository.Neo4jRepository;

public interface DogRepository extends Neo4jRepository<Dog, String> {}
