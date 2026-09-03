package com.example.arimaabackend.services;

import com.example.arimaabackend.model.sql.PositionEntity;

public interface PositionService {
    PositionEntity getOrCreatePosition(Integer id, String color, String piece, String coordinate);
}
