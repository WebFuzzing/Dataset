package com.example.arimaabackend.migration.steps.mongodb;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.model.mongo.PuzzleDocument;
import com.example.arimaabackend.model.sql.PuzzleEntity;
import com.example.arimaabackend.repository.mongo.PuzzleMongoRepository;
import com.example.arimaabackend.repository.sql.PuzzleJpaRepository;

@Service
@Profile("migration")
public class PuzzleMongoMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(PuzzleMongoMigration.class);

    private final PuzzleJpaRepository puzzleJpaRepository;
    private final PuzzleMongoRepository puzzleMongoRepository;
    private final JdbcTemplate jdbcTemplate;

    public PuzzleMongoMigration(
        PuzzleJpaRepository puzzleJpaRepository,
        PuzzleMongoRepository puzzleMongoRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.puzzleJpaRepository = puzzleJpaRepository;
        this.puzzleMongoRepository = puzzleMongoRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String stepName() {
        return "puzzle-mongo";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.MONGODB);
    }

    @Override
    public int getOrder() {
        return 124;
    }

    @Override
    @Transactional(readOnly = true)
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), puzzleJpaRepository.count());
            return;
        }
        Map<Integer, List<PuzzleDocument.Position>> openingByPuzzleId = loadPuzzleOpenings();
        Map<Integer, List<PuzzleDocument.SolutionMove>> solutionsByPuzzleId = loadPuzzleSolutions();
        List<PuzzleDocument> documents = puzzleJpaRepository.findAll().stream()
            .map(p -> toDocument(
                    p,
                    openingByPuzzleId.get(p.getId()),
                    solutionsByPuzzleId.getOrDefault(p.getId(), List.of())))
            .toList();
        puzzleMongoRepository.saveAll(documents);
        log.info("[{}] migrated {} puzzles", stepName(), documents.size());
    }

    private PuzzleDocument toDocument(
            PuzzleEntity e,
            List<PuzzleDocument.Position> opening,
            List<PuzzleDocument.SolutionMove> solutions) {
        var d = new PuzzleDocument();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setObjective(e.getObjective());
        d.setPlayerSide(e.getPlayerSide());
        d.setRounds(e.getRounds());
        d.setOpening(opening);
        d.setSolution(solutions);
        return d;
    }

    private Map<Integer, List<PuzzleDocument.SolutionMove>> loadPuzzleSolutions() {
        var grouped = new HashMap<Integer, List<PuzzleDocument.SolutionMove>>();
        jdbcTemplate.query(
            """
            SELECT s.puzzles_id, s.turn, s.sequence, s.direction, s.status,
                   p.color, p.piece, p.cordinate
            FROM Solutions s
            JOIN Position p ON p.id = s.position_id
            ORDER BY s.puzzles_id ASC, s.turn ASC, s.sequence ASC
            """,
            rs -> {
                int puzzleId = rs.getInt("puzzles_id");
                var move = new PuzzleDocument.SolutionMove();
                move.setTurn(rs.getInt("turn"));
                move.setSequence(rs.getInt("sequence"));
                move.setDirection(rs.getString("direction"));
                move.setStatus(rs.getString("status"));
                move.setPosition(positionRow(
                    rs.getString("color"),
                    rs.getString("piece"),
                    rs.getString("cordinate")
                ));
                grouped.computeIfAbsent(puzzleId, ignored -> new java.util.ArrayList<>()).add(move);
            }
        );
        return grouped;
    }

    private Map<Integer, List<PuzzleDocument.Position>> loadPuzzleOpenings() {
        var grouped = new HashMap<Integer, List<PuzzleDocument.Position>>();
        jdbcTemplate.query(
            """
            SELECT obp.puzzles_id, p.color, p.piece, p.cordinate
            FROM OpeningsByPuzzle obp
            JOIN Position p ON p.id = obp.position_id
            ORDER BY obp.id ASC
            """,
            rs -> {
                int puzzleId = rs.getInt("puzzles_id");
                grouped.computeIfAbsent(puzzleId, ignored -> new java.util.ArrayList<>()).add(positionRow(
                    rs.getString("color"),
                    rs.getString("piece"),
                    rs.getString("cordinate")
                ));
            }
        );
        return grouped;
    }

    private PuzzleDocument.Position positionRow(String color, String piece, String coordinate) {
        var p = new PuzzleDocument.Position();
        p.setColor(color);
        p.setPiece(piece);
        p.setCoordinate(coordinate);
        return p;
    }
}

