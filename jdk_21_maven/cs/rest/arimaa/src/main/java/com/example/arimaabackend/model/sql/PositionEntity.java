package com.example.arimaabackend.model.sql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Position")
public class PositionEntity {

    @Id
    private Integer id;

    @Column(nullable = false, length = 6)
    private String color;

    @Column(nullable = false, length = 4)
    private String piece;

    @Column(name = "cordinate", nullable = false, length = 2)
    private String coordinate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

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