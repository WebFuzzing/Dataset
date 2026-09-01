package com.example.arimaabackend.repository.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.arimaabackend.model.sql.PositionEntity;

public interface PositionJpaRepository extends JpaRepository<PositionEntity, Integer> {
    @Modifying
    @Query("DELETE FROM PositionEntity p WHERE p.id IN (SELECT m.position.id FROM MoveEntity m WHERE m.match.id = :matchId) " +
            "AND NOT EXISTS (SELECT 1 FROM MoveEntity m2 WHERE m2.position.id = p.id AND m2.match.id <> :matchId) " +
            "AND NOT EXISTS (SELECT 1 FROM SolutionEntity s WHERE s.position.id = p.id)")
    void deleteUnusedByMatchId(@Param("matchId") Integer matchId);
}