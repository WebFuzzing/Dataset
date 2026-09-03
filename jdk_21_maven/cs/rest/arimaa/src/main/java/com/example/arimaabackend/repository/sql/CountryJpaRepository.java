package com.example.arimaabackend.repository.sql;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.arimaabackend.model.sql.CountryEntity;

public interface CountryJpaRepository extends JpaRepository<CountryEntity, Integer> {
    Optional<CountryEntity> findByName(String name);
}