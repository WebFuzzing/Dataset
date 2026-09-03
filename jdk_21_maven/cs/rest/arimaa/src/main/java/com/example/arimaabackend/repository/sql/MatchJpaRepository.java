package com.example.arimaabackend.repository.sql;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.arimaabackend.model.sql.MatchEntity;

public interface MatchJpaRepository extends JpaRepository<MatchEntity, Integer> {

    @Query("""
            SELECT m FROM MatchEntity m
            JOIN FETCH m.goldPlayer
            JOIN FETCH m.silverPlayer
            JOIN FETCH m.event
            JOIN FETCH m.gameType
            """)
    List<MatchEntity> findAllForMigration();
    List<MatchEntity> findBySilverPlayer_Id(Integer playerId);

    List<MatchEntity> findByGoldPlayer_Id(Integer playerId);

    List<MatchEntity> findByEvent_Id(Integer eventId);

    List<MatchEntity> findByGameType_Id(Integer gameTypeId);
}