package com.example.arimaabackend.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Puzzles")
public class PuzzleEntity {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 45)
    private String name;

    @Column(nullable = false, length = 45)
    private String objective;

    @Column(name = "playerSide", nullable = false, length = 6)
    private String playerSide;

    @Column(nullable = false)
    private Integer rounds;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getPlayerSide() {
        return playerSide;
    }

    public void setPlayerSide(String playerSide) {
        this.playerSide = playerSide;
    }

    public Integer getRounds() {
        return rounds;
    }

    public void setRounds(Integer rounds) {
        this.rounds = rounds;
    }
}