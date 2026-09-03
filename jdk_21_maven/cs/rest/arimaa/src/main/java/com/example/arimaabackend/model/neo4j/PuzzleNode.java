package com.example.arimaabackend.model.neo4j;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Puzzle")
public class PuzzleNode {

    @Id
    private Integer id;

    private String name;
    private String objective;
    private String playerSide;
    private Integer rounds;

    @Relationship(type = "HAS_SOLUTION")
    private List<SolutionNode> solutions;

    @Relationship(type = "HAS_OPENING")
    private List<OpeningNode> openings;

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

    public List<SolutionNode> getSolutions() {
        return solutions;
    }

    public void setSolutions(List<SolutionNode> solutions) {
        this.solutions = solutions;
    }

    public List<OpeningNode> getOpenings() {
        return openings;
    }

    public void setOpenings(List<OpeningNode> openings) {
        this.openings = openings;
    }
}

