package com.example.arimaabackend.model.neo4j;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("MoveList")
public class MoveListNode {

    @Id
    private String key;

    @Relationship(type = "HAS_TURN")
    private List<TurnNode> turns;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<TurnNode> getTurns() {
        return turns;
    }

    public void setTurns(List<TurnNode> turns) {
        this.turns = turns;
    }
}

