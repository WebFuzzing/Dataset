package com.example.arimaabackend.model.neo4j;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Turn")
public class TurnNode {

    @Id
    private String key;

    private Integer matchId;
    private Integer turn;

    @Relationship(type = "HAS_MOVE")
    private List<MoveNode> moves;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getMatchId() {
        return matchId;
    }

    public void setMatchId(Integer matchId) {
        this.matchId = matchId;
    }

    public Integer getTurn() {
        return turn;
    }

    public void setTurn(Integer turn) {
        this.turn = turn;
    }

    public List<MoveNode> getMoves() {
        return moves;
    }

    public void setMoves(List<MoveNode> moves) {
        this.moves = moves;
    }
}

