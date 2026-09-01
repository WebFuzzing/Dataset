package com.example.arimaabackend.repository.sql;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.arimaabackend.model.sql.SolutionEntity;

public interface SolutionJpaRepository extends JpaRepository<SolutionEntity, Integer> {
    List<SolutionEntity> findByPuzzle_IdOrderByTurnAscSequenceAsc(Integer puzzleId);
}