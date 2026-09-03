package com.example.arimaabackend.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Event")
public class EventNode {

    @Id
    private Integer id;

    private String name;
    private Boolean rated;

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

    public Boolean getRated() {
        return rated;
    }

    public void setRated(Boolean rated) {
        this.rated = rated;
    }
}

