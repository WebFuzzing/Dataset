package com.example.arimaabackend.repository.sql;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.arimaabackend.model.sql.MoveEntity;

public interface MoveJpaRepository extends JpaRepository<MoveEntity, Integer> {
    List<MoveEntity> findByMatch_IdOrderByTurnAscSequenceAsc(Integer matchId);

    void deleteByMatch_Id(Integer matchId);
}