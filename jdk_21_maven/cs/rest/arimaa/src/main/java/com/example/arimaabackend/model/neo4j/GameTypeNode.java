package com.example.arimaabackend.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("GameType")
public class GameTypeNode {

    @Id
    private Integer id;

    private String name;
    private Integer timeIncrement;
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

