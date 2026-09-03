package com.example.arimaabackend.services;

import com.example.arimaabackend.model.sql.MatchEntity;
import com.example.arimaabackend.model.sql.MoveEntity;
import com.example.arimaabackend.model.sql.PositionEntity;
import com.example.arimaabackend.repository.sql.MoveJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MoveServiceImpl implements MoveService {

    private final MoveJpaRepository moveRepository;

    public MoveServiceImpl(MoveJpaRepository moveRepository) {
        this.moveRepository = moveRepository;
    }

    @Override
    @Transactional
    public MoveEntity createMove(Integer id, Integer turn, Integer sequence, String direction, String status, MatchEntity match, PositionEntity position) {
        MoveEntity move = new MoveEntity();
        move.setId(id);
        move.setTurn(turn);
        move.setSequence(sequence);
        move.setDirection(direction);
        move.setStatus(status);
        move.setMatch(match);
        move.setPosition(position);
        return moveRepository.save(move);
    }
}
