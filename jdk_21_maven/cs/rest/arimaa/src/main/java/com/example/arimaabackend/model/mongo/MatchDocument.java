package com.example.arimaabackend.model.mongo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("matches")
public class MatchDocument {

    @Id
    private Integer id;

    private String terminationType;
    private String matchResult;
    private Integer goldRating;
    private Integer silverRating;
    private Instant timestamp;

    private List<PlayerRef> players = new ArrayList<>();

    private Event event;
    private GameType gameType;

    private List<Position> opening = new ArrayList<>();
    private List<Move> moves = new ArrayList<>();

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

    public List<PlayerRef> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerRef> players) {
        this.players = players != null ? players : new ArrayList<>();
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public List<Position> getOpening() {
        return opening;
    }

    public void setOpening(List<Position> opening) {
        this.opening = opening != null ? opening : new ArrayList<>();
    }

    public List<Move> getMoves() {
        return moves;
    }

    public void setMoves(List<Move> moves) {
        this.moves = moves != null ? moves : new ArrayList<>();
    }

    public static class PlayerRef {
        private Integer playerId;
        private String color;

        public Integer getPlayerId() {
            return playerId;
        }

        public void setPlayerId(Integer playerId) {
            this.playerId = playerId;
        }

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }
    }

    public static class Event {
        private Integer id;
        private String name;
        private Boolean isRated;

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

        public Boolean getIsRated() {
            return isRated;
        }

        public void setIsRated(Boolean isRated) {
            this.isRated = isRated;
        }
    }

    public static class GameType {
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

    public static class Position {
        private String color;
        private String piece;
        private String coordinate;

        public String getColor() {
            return color;
        }

        public void setColor(String color) {
            this.color = color;
        }

        public String getPiece() {
            return piece;
        }

        public void setPiece(String piece) {
            this.piece = piece;
        }

        public String getCoordinate() {
            return coordinate;
        }

        public void setCoordinate(String coordinate) {
            this.coordinate = coordinate;
        }
    }

    public static class Move {
        private Integer turn;
        private Integer sequence;
        private String direction;
        private String status;
        private Position position;

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

        public Position getPosition() {
            return position;
        }

        public void setPosition(Position position) {
            this.position = position;
        }
    }
}
