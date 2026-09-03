package com.example.arimaabackend.repository.sql;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.arimaabackend.model.sql.EventEntity;

public interface EventJpaRepository extends JpaRepository<EventEntity, Integer> {
    Optional<EventEntity> findByName(String name);
}