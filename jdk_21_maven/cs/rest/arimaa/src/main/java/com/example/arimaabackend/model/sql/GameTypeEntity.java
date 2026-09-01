package com.example.arimaabackend.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "GameTypes")
public class GameTypeEntity {

    @Id
    private Integer id;

    @Column(nullable = false, length = 45)
    private String name;

    @Column(name = "time_increment")
    private Integer timeIncrement;

    @Column(name = "time_reserve")
    private Integer timeReserve;

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

    public Integer getTimeIncrement() {
        return timeIncrement;
    }

    public void setTimeIncrement(Integer timeIncrement) {
        this.timeIncrement = timeIncrement;
    }

    public Integer getTimeReserve() {
        return timeReserve;
    }

    public void setTimeReserve(Integer timeReserve) {
        this.timeReserve = timeReserve;
    }
}