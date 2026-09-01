package com.example.arimaabackend.services;

import com.example.arimaabackend.model.sql.MatchEntity;
import com.example.arimaabackend.model.sql.MoveEntity;
import com.example.arimaabackend.model.sql.PositionEntity;

public interface MoveService {
    MoveEntity createMove(Integer id, Integer turn, Integer sequence, String direction, String status, MatchEntity match, PositionEntity position);
}
