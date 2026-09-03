package com.example.arimaabackend.repository.sql;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.arimaabackend.model.sql.PlayerEntity;

public interface PlayerJpaRepository extends JpaRepository<PlayerEntity, Integer> {

    @Query("SELECT p FROM PlayerEntity p JOIN FETCH p.user JOIN FETCH p.country")
    List<PlayerEntity> findAllWithUserAndCountry();
    Optional<PlayerEntity> findByUser_Id(Long id);

    Optional<PlayerEntity> findByUser_Username(String username);

    Optional<PlayerEntity> findByUser_Email(String email);

    List<PlayerEntity> findByCountryName(String countryName);

}