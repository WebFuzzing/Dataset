package com.example.arimaabackend.model.neo4j;

import java.util.List;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Opening")
public class OpeningNode {

    @Id
    private String key;

    private String side;
    private String parentType;
    private Integer parentId;

    @Relationship(type = "HAS_POSITION")
    private List<PositionNode> positions;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
    }

    public Integer getParentId() {
        return parentId;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }

    public List<PositionNode> getPositions() {
        return positions;
    }

    public void setPositions(List<PositionNode> positions) {
        this.positions = positions;
    }
}

