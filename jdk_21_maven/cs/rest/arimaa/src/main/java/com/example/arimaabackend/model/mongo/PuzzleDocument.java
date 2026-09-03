package com.example.arimaabackend.model.mongo;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("puzzles")
public class PuzzleDocument {

    @Id
    private Integer id;

    private String name;
    private String objective;
    private String playerSide;
    private Integer rounds;

    private List<Position> opening = new ArrayList<>();
    private List<SolutionMove> solution = new ArrayList<>();

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

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getPlayerSide() {
        return playerSide;
    }

    public void setPlayerSide(String playerSide) {
        this.playerSide = playerSide;
    }

    public Integer getRounds() {
        return rounds;
    }

    public void setRounds(Integer rounds) {
        this.rounds = rounds;
    }

    public List<Position> getOpening() {
        return opening;
    }

    public void setOpening(List<Position> opening) {
        this.opening = opening != null ? opening : new ArrayList<>();
    }

    public List<SolutionMove> getSolution() {
        return solution;
    }

    public void setSolution(List<SolutionMove> solution) {
        this.solution = solution != null ? solution : new ArrayList<>();
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

    public static class SolutionMove {
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

