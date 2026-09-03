package com.example.arimaabackend.migration.support;

import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Parameterized Cypher for edges not modeled from the child node side.
 */
@Component
@Profile("migration")
public class Neo4jLinkSupport {

    private final Neo4jClient neo4jClient;

    public Neo4jLinkSupport(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    public void mergeHasMoveEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (m:Match {id: row.matchId})
                        MATCH (v:Move {id: row.moveId})
                        WITH row, m, v, "matchMoves:" + toString(row.matchId) AS moveListKey
                        MERGE (ml:MoveList {key: moveListKey})
                        MERGE (m)-[:HAS_MOVELIST]->(ml)
                        WITH row, ml, v, "matchTurn:" + toString(row.matchId) + ":" + toString(v.turn) AS turnKey
                        MERGE (t:Turn {key: turnKey})
                        SET t.matchId = row.matchId,
                            t.turn = v.turn
                        MERGE (ml)-[:HAS_TURN]->(t)
                        MERGE (t)-[:HAS_MOVE]->(v)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergeMovePositionEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (v:Move {id: row.moveId})
                        MATCH (p:Position {id: row.positionId})
                        MERGE (v)-[:AT_POSITION]->(p)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    /**
     * Fast-path: merge Move nodes and all required edges in one UNWIND.
     * Expected row keys: matchId, moveId, turn, sequence, direction, status, positionId
     */
    public void mergeMovesBatch(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MERGE (v:Move {id: row.moveId})
                        SET v.turn = row.turn,
                            v.sequence = row.sequence,
                            v.direction = row.direction,
                            v.status = row.status
                        WITH row, v
                        MATCH (p:Position {id: row.positionId})
                        MERGE (v)-[:AT_POSITION]->(p)
                        WITH row, v
                        MATCH (m:Match {id: row.matchId})
                        WITH row, m, v, "matchMoves:" + toString(row.matchId) AS moveListKey
                        MERGE (ml:MoveList {key: moveListKey})
                        MERGE (m)-[:HAS_MOVELIST]->(ml)
                        WITH row, ml, v, "matchTurn:" + toString(row.matchId) + ":" + toString(row.turn) AS turnKey
                        MERGE (t:Turn {key: turnKey})
                        SET t.matchId = row.matchId,
                            t.turn = row.turn
                        MERGE (ml)-[:HAS_TURN]->(t)
                        MERGE (t)-[:HAS_MOVE]->(v)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergeHasSolutionEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (z:Puzzle {id: row.puzzleId})
                        MATCH (s:Solution {id: row.solutionId})
                        MERGE (z)-[:HAS_SOLUTION]->(s)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergeSolutionPositionEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (s:Solution {id: row.solutionId})
                        MATCH (p:Position {id: row.positionId})
                        MERGE (s)-[:AT_POSITION]->(p)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergePlayerCountryEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (p:Player {id: row.playerId})
                        MATCH (c:Country {id: row.countryId})
                        MERGE (p)-[:IN_COUNTRY]->(c)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergePlayerUserEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (p:Player {id: row.playerId})
                        MATCH (u:User {id: row.userId})
                        MERGE (p)-[:HAS_USER]->(u)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergeMatchReferenceEdges(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (m:Match {id: row.matchId})
                        MATCH (silver:Player {id: row.silverPlayerId})
                        MATCH (gold:Player {id: row.goldPlayerId})
                        MATCH (e:Event {id: row.eventId})
                        MATCH (g:GameType {id: row.gameTypeId})
                        MERGE (m)-[:HAS_PLAYER {side: "SILVER"}]->(silver)
                        MERGE (m)-[:HAS_PLAYER {side: "GOLD"}]->(gold)
                        MERGE (m)-[:IN_EVENT]->(e)
                        MERGE (m)-[:HAS_GAMETYPE]->(g)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergeOpeningByMatch(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (m:Match {id: row.matchId})
                        MATCH (p:Position {id: row.positionId})
                        WITH row, m, p, coalesce(p.color, "UNKNOWN") AS side
                        WITH row, m, p, side, "match:" + toString(row.matchId) + ":" + side AS openingKey
                        MERGE (o:Opening {key: openingKey})
                        SET o.side = side,
                            o.parentType = "MATCH",
                            o.parentId = row.matchId
                        MERGE (m)-[:HAS_OPENING]->(o)
                        MERGE (o)-[:HAS_POSITION]->(p)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }

    public void mergeOpeningByPuzzle(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) {
            return;
        }
        neo4jClient
                .query(
                        """
                        UNWIND $rows AS row
                        MATCH (z:Puzzle {id: row.puzzleId})
                        MATCH (p:Position {id: row.positionId})
                        WITH row, z, p, coalesce(p.color, "UNKNOWN") AS side
                        WITH row, z, p, side, "puzzle:" + toString(row.puzzleId) + ":" + side AS openingKey
                        MERGE (o:Opening {key: openingKey})
                        SET o.side = side,
                            o.parentType = "PUZZLE",
                            o.parentId = row.puzzleId
                        MERGE (z)-[:HAS_OPENING]->(o)
                        MERGE (o)-[:HAS_POSITION]->(p)
                        """
                )
                .bindAll(Map.of("rows", rows))
                .run();
    }
}
