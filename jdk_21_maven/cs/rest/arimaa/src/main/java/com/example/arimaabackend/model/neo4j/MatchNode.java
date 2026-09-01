package com.example.arimaabackend.model.neo4j;

import java.time.Instant;
import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Match")
public class MatchNode {

    @Id
    private Integer id;

    private String terminationType;
    private String matchResult;
    private Integer goldRating;
    private Integer silverRating;
    private Instant timestamp;

    @Relationship(type = "HAS_PLAYER")
    private List<MatchPlayerRelationship> players;

    @Relationship(type = "IN_EVENT")
    private EventNode event;

    @Relationship(type = "HAS_GAMETYPE")
    private GameTypeNode gameType;

    @Relationship(type = "HAS_MOVELIST")
    private MoveListNode moveList;

    @Relationship(type = "HAS_OPENING")
    private List<OpeningNode> openings;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTerminationType() {
        return terminationType;
    }

    public void setTerminationType(String terminationType) {
        this.terminationType = terminationType;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public Integer getGoldRating() {
        return goldRating;
    }

    public void setGoldRating(Integer goldRating) {
        this.goldRating = goldRating;
    }

    public Integer getSilverRating() {
        return silverRating;
    }

    public void setSilverRating(Integer silverRating) {
        this.silverRating = silverRating;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<MatchPlayerRelationship> getPlayers() {
        return players;
    }

    public void setPlayers(List<MatchPlayerRelationship> players) {
        this.players = players;
    }

    public EventNode getEvent() {
        return event;
    }

    public void setEvent(EventNode event) {
        this.event = event;
    }

    public GameTypeNode getGameType() {
        return gameType;
    }

    public void setGameType(GameTypeNode gameType) {
        this.gameType = gameType;
    }

    public MoveListNode getMoveList() {
        return moveList;
    }

    public void setMoveList(MoveListNode moveList) {
        this.moveList = moveList;
    }

    public List<OpeningNode> getOpenings() {
        return openings;
    }

    public void setOpenings(List<OpeningNode> openings) {
        this.openings = openings;
    }
}

