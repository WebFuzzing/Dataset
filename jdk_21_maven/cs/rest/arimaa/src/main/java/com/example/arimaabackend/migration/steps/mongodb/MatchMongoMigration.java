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
import com.example.arimaabackend.model.mongo.MatchDocument;
import com.example.arimaabackend.model.sql.MatchEntity;
import com.example.arimaabackend.repository.mongo.MatchMongoRepository;
import com.example.arimaabackend.repository.sql.MatchJpaRepository;

@Service
@Profile("migration")
public class MatchMongoMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(MatchMongoMigration.class);

    private final MatchMongoRepository matchMongoRepository;
    private final MatchJpaRepository matchJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    public MatchMongoMigration(
        MatchMongoRepository matchMongoRepository,
        MatchJpaRepository matchJpaRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.matchMongoRepository = matchMongoRepository;
        this.matchJpaRepository = matchJpaRepository;
        this.jdbcTemplate = jdbcTemplate;
    }


    @Override
    public String stepName() {
        return "match-mongo";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.MONGODB);
    }

    @Override
    public int getOrder() {
        return 123;
    }

    @Override
    @Transactional(readOnly = true)
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), matchJpaRepository.count());
            return;
        }
        Map<Integer, List<MatchDocument.Position>> openingByMatchId = loadMatchOpenings();
        Map<Integer, List<MatchDocument.Move>> movesByMatchId = loadMatchMoves();
        List<MatchDocument> documents = matchJpaRepository.findAllForMigration().stream()
            .map(m -> toDocument(
                    m,
                    openingByMatchId.get(m.getId()),
                    movesByMatchId.getOrDefault(m.getId(), List.of())))
            .toList();
        matchMongoRepository.saveAll(documents);
        log.info("[{}] migrated {} matches", stepName(), documents.size());
    }

    private MatchDocument toDocument(
            MatchEntity e,
            List<MatchDocument.Position> opening,
            List<MatchDocument.Move> moves) {
        var d = new MatchDocument();
        d.setId(e.getId());
        d.setTerminationType(e.getTerminationType());
        d.setMatchResult(e.getMatchResult());
        d.setGoldRating(e.getGoldRating());
        d.setSilverRating(e.getSilverRating());
        d.setTimestamp(e.getTimestamp());

        d.setPlayers(List.of(
            playerRef(e.getGoldPlayer().getId(), "gold"),
            playerRef(e.getSilverPlayer().getId(), "silver")
        ));

        d.setEvent(event(e));
        d.setGameType(gameType(e));
        d.setOpening(opening);
        d.setMoves(moves);
        return d;
    }

    private MatchDocument.PlayerRef playerRef(Integer playerId, String color) {
        var p = new MatchDocument.PlayerRef();
        p.setPlayerId(playerId);
        p.setColor(color);
        return p;
    }

    private MatchDocument.Event event(MatchEntity e) {
        var src = e.getEvent();
        if (src == null) {
            return null;
        }
        var ev = new MatchDocument.Event();
        ev.setId(src.getId());
        ev.setName(src.getName());
        ev.setIsRated(src.getRated());
        return ev;
    }

    private MatchDocument.GameType gameType(MatchEntity e) {
        var src = e.getGameType();
        if (src == null) {
            return null;
        }
        var gt = new MatchDocument.GameType();
        gt.setId(src.getId());
        gt.setName(src.getName());
        gt.setTimeIncrement(src.getTimeIncrement());
        gt.setTimeReserve(src.getTimeReserve());
        return gt;
    }

    private Map<Integer, List<MatchDocument.Move>> loadMatchMoves() {
        var grouped = new HashMap<Integer, List<MatchDocument.Move>>();
        jdbcTemplate.query(
            """
            SELECT mv.matches_id, mv.turn, mv.sequence, mv.direction, mv.status,
                   p.color, p.piece, p.cordinate
            FROM Moves mv
            JOIN Position p ON p.id = mv.position_id
            ORDER BY mv.matches_id ASC, mv.turn ASC, mv.sequence ASC
            """,
            rs -> {
                int matchId = rs.getInt("matches_id");
                var move = new MatchDocument.Move();
                move.setTurn(rs.getInt("turn"));
                move.setSequence(rs.getInt("sequence"));
                move.setDirection(rs.getString("direction"));
                move.setStatus(rs.getString("status"));
                move.setPosition(positionRow(
                    rs.getString("color"),
                    rs.getString("piece"),
                    rs.getString("cordinate")
                ));
                grouped.computeIfAbsent(matchId, ignored -> new java.util.ArrayList<>()).add(move);
            }
        );
        return grouped;
    }

    private Map<Integer, List<MatchDocument.Position>> loadMatchOpenings() {
        var grouped = new HashMap<Integer, List<MatchDocument.Position>>();
        jdbcTemplate.query(
            """
            SELECT obm.matches_id, p.color, p.piece, p.cordinate
            FROM OpeningsByMatch obm
            JOIN Position p ON p.id = obm.position_id
            ORDER BY obm.id ASC
            """,
            rs -> {
                int matchId = rs.getInt("matches_id");
                grouped.computeIfAbsent(matchId, ignored -> new java.util.ArrayList<>()).add(positionRow(
                    rs.getString("color"),
                    rs.getString("piece"),
                    rs.getString("cordinate")
                ));
            }
        );
        return grouped;
    }

    private MatchDocument.Position positionRow(String color, String piece, String coordinate) {
        var p = new MatchDocument.Position();
        p.setColor(color);
        p.setPiece(piece);
        p.setCoordinate(coordinate);
        return p;
    }



    
    
}
