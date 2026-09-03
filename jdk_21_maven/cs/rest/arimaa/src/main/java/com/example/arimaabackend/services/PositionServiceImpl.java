package com.example.arimaabackend.services;

import com.example.arimaabackend.model.sql.PositionEntity;
import com.example.arimaabackend.repository.sql.PositionJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PositionServiceImpl implements PositionService {

    private final PositionJpaRepository positionRepository;

    public PositionServiceImpl(PositionJpaRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    @Override
    @Transactional
    public PositionEntity getOrCreatePosition(Integer id, String color, String piece, String coordinate) {
        return positionRepository.findById(id).orElseGet(() -> {
            PositionEntity position = new PositionEntity();
            position.setId(id);
            position.setColor(color);
            position.setPiece(piece);
            position.setCoordinate(coordinate);
            return positionRepository.save(position);
        });
    }
}
