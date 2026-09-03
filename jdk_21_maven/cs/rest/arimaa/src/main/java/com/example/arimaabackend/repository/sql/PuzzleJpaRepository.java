package com.example.arimaabackend.repository.sql;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.arimaabackend.model.sql.PuzzleEntity;

public interface PuzzleJpaRepository extends JpaRepository<PuzzleEntity, Integer> {
    Optional<PuzzleEntity> findByName(String name);
}