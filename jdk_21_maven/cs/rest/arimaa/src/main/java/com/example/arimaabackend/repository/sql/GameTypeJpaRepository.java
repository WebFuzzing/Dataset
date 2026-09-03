package com.example.arimaabackend.repository.sql;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.arimaabackend.model.sql.GameTypeEntity;

public interface GameTypeJpaRepository extends JpaRepository<GameTypeEntity, Integer> {
    Optional<GameTypeEntity> findByName(String name);
}