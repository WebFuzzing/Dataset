package com.example.arimaabackend.model.neo4j;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class MatchPlayerRelationship {

    public enum Side {
        SILVER,
        GOLD
    }

    @Id
    @GeneratedValue
    private Long id;

    private Side side;

    @TargetNode
    private PlayerNode player;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public PlayerNode getPlayer() {
        return player;
    }

    public void setPlayer(PlayerNode player) {
        this.player = player;
    }
}

