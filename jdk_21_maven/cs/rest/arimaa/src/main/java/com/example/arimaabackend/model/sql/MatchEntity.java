package com.example.arimaabackend.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "Matches")
public class MatchEntity {

    @Id
    private Integer id;

    @Column(name = "termination_type", nullable = false, length = 45)
    private String terminationType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id_silver", nullable = false)
    private PlayerEntity silverPlayer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "player_id_gold", nullable = false)
    private PlayerEntity goldPlayer;

    @Column(name = "gold_rating", nullable = true, length = 45)
    private Integer goldRating;

    @Column(name = "silver_rating", nullable = true, length = 45)
    private Integer silverRating;

    @Column(name = "match_result", nullable = false, length = 45)
    private String matchResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "events_id", nullable = false)
    private EventEntity event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gameTypes_id", nullable = false)
    private GameTypeEntity gameType;

    @Column(name = "timestamp")
    private Instant timestamp;

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

    public PlayerEntity getSilverPlayer() {
        return silverPlayer;
    }

    public void setSilverPlayer(PlayerEntity silverPlayer) {
        this.silverPlayer = silverPlayer;
    }

    public PlayerEntity getGoldPlayer() {
        return goldPlayer;
    }

    public void setGoldPlayer(PlayerEntity goldPlayer) {
        this.goldPlayer = goldPlayer;
    }

    public Integer getGoldRating() {
        return this.goldRating;
    }

    public void setGoldRating(Integer goldRating) {
        this.goldRating = goldRating;
    }

    public Integer getSilverRating() {
        return this.silverRating;
    }

    public void setSilverRating(Integer silverRating) {
        this.silverRating = silverRating;
    }

    public String getMatchResult() {
        return matchResult;
    }

    public void setMatchResult(String matchResult) {
        this.matchResult = matchResult;
    }

    public EventEntity getEvent() {
        return event;
    }

    public void setEvent(EventEntity event) {
        this.event = event;
    }

    public GameTypeEntity getGameType() {
        return gameType;
    }

    public void setGameType(GameTypeEntity gameType) {
        this.gameType = gameType;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}