package com.example.arimaabackend.services.neo4j;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.example.arimaabackend.dto.EventSummary;
import com.example.arimaabackend.dto.GameTypeSummary;
import com.example.arimaabackend.dto.MatchResponse;
import com.example.arimaabackend.dto.PlayerSummary;
import com.example.arimaabackend.model.neo4j.EventNode;
import com.example.arimaabackend.model.neo4j.GameTypeNode;
import com.example.arimaabackend.model.neo4j.MatchNode;
import com.example.arimaabackend.model.neo4j.MatchPlayerRelationship;
import com.example.arimaabackend.model.neo4j.PlayerNode;
import com.example.arimaabackend.model.neo4j.UserNode;
import com.example.arimaabackend.repository.neo4j.MatchNeo4jRepository;

@Service
public class MatchNeo4jServiceImpl implements MatchNeo4jService {

    private final MatchNeo4jRepository matchNeo4jRepository;

    public MatchNeo4jServiceImpl(MatchNeo4jRepository matchNeo4jRepository) {
        this.matchNeo4jRepository = matchNeo4jRepository;
    }

    @Override
    public Optional<MatchResponse> getMatch(Integer id) {
        return matchNeo4jRepository.findById(id).map(this::toMatchResponse);
    }

    private MatchResponse toMatchResponse(MatchNode node) {
        PlayerSummary silver = null;
        PlayerSummary gold = null;

        if (node.getPlayers() != null) {
            for (MatchPlayerRelationship rel : node.getPlayers()) {
                PlayerSummary summary = toPlayerSummary(rel.getPlayer());
                if (rel.getSide() == MatchPlayerRelationship.Side.SILVER) {
                    silver = summary;
                } else if (rel.getSide() == MatchPlayerRelationship.Side.GOLD) {
                    gold = summary;
                }
            }
        }

        EventNode event = node.getEvent();
        EventSummary eventSummary = event != null
                ? new EventSummary(event.getId(), event.getName())
                : null;

        GameTypeNode gameType = node.getGameType();
        GameTypeSummary gameTypeSummary = gameType != null
                ? new GameTypeSummary(
                        gameType.getId(),
                        gameType.getName(),
                        gameType.getTimeIncrement(),
                        gameType.getTimeReserve())
                : null;

        return new MatchResponse(
                node.getId(),
                node.getTerminationType(),
                silver,
                gold,
                node.getGoldRating(),
                node.getSilverRating(),
                node.getMatchResult(),
                eventSummary,
                gameTypeSummary,
                node.getTimestamp()
        );
    }

    private PlayerSummary toPlayerSummary(PlayerNode player) {
        if (player == null) {
            return null;
        }
        UserNode user = player.getUser();
        return new PlayerSummary(
                player.getId(),
                user != null ? user.getUsername() : "Unknown"
        );
    }
}
