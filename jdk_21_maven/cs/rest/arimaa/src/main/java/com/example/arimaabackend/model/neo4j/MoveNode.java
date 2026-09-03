package com.example.arimaabackend.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Move")
public class MoveNode {

    @Id
    private Integer id;

    private Integer turn;
    private Integer sequence;
    private String direction;
    private String status;

    @Relationship(type = "AT_POSITION")
    private PositionNode position;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTurn() {
        return turn;
    }

    public void setTurn(Integer turn) {
        this.turn = turn;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PositionNode getPosition() {
        return position;
    }

    public void setPosition(PositionNode position) {
        this.position = position;
    }
}

