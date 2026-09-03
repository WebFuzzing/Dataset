package com.example.arimaabackend.services.mongo;

import com.example.arimaabackend.dto.*;
import com.example.arimaabackend.model.mongo.MatchDocument;
import com.example.arimaabackend.model.mongo.PlayerDocument;
import com.example.arimaabackend.model.mongo.UserDocument;
import com.example.arimaabackend.repository.mongo.MatchMongoRepository;
import com.example.arimaabackend.repository.mongo.PlayerMongoRepository;
import com.example.arimaabackend.repository.mongo.UserMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class MatchMongoServiceImpl implements MatchMongoService {

    private final MatchMongoRepository matchMongoRepository;
    private final PlayerMongoRepository playerMongoRepository;
    private final UserMongoRepository userMongoRepository;

    public MatchMongoServiceImpl(
            MatchMongoRepository matchMongoRepository,
            PlayerMongoRepository playerMongoRepository,
            UserMongoRepository userMongoRepository) {
        this.matchMongoRepository = matchMongoRepository;
        this.playerMongoRepository = playerMongoRepository;
        this.userMongoRepository = userMongoRepository;
    }

    @Override
    public Optional<MatchResponse> getMatch(Integer id) {
        return matchMongoRepository.findById(id).map(this::toMatchResponse);
    }

    @Override
    public MatchResponse createMatch(String matchData) {
        String[] parts = splitAndValidateMatchData(matchData);
        Integer id = Integer.parseInt(parts[0]);

        MatchDocument doc = new MatchDocument();
        doc.setId(id);
        populateMatchFromParts(doc, parts);

        return toMatchResponse(matchMongoRepository.save(doc));
    }

    @Override
    public MatchResponse updateMatch(Integer id, String matchData) {
        MatchDocument doc = matchMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match %d not found in Mongo".formatted(id)));

        String[] parts = splitAndValidateMatchData(matchData);
        populateMatchFromParts(doc, parts);

        return toMatchResponse(matchMongoRepository.save(doc));
    }

    @Override
    public void deleteMatch(Integer id) {
        if (!matchMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match %d not found in Mongo".formatted(id));
        }
        matchMongoRepository.deleteById(id);
    }

    private String[] splitAndValidateMatchData(String matchData) {
        String[] parts = matchData.split("\t");
        if (parts.length < 23) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid match data format: expected 23+ parts, got " + parts.length);
        }
        return parts;
    }

    private void populateMatchFromParts(MatchDocument doc, String[] parts) {
        Integer wPlayerId = Integer.parseInt(parts[1]);
        Integer bPlayerId = Integer.parseInt(parts[2]);
        String wUsername = parts[3];
        String bUsername = parts[4];
        String wCountryName = parts[7];
        String bCountryName = parts[8];
        Integer wRating = Integer.parseInt(parts[9]);
        Integer bRating = Integer.parseInt(parts[10]);
        String eventName = parts[15];
        String timeControl = parts[17];
        long startTs = Long.parseLong(parts[19]);
        String result = parts[21];
        String termination = parts[22];

        getOrCreatePlayer(wPlayerId, wUsername, wCountryName, wRating);
        getOrCreatePlayer(bPlayerId, bUsername, bCountryName, bRating);

        doc.setGoldRating(wRating);
        doc.setSilverRating(bRating);
        doc.setTimestamp(Instant.ofEpochSecond(startTs));
        doc.setMatchResult(result);
        doc.setTerminationType(termination);

        List<MatchDocument.PlayerRef> playerRefs = new ArrayList<>();
        MatchDocument.PlayerRef goldRef = new MatchDocument.PlayerRef();
        goldRef.setPlayerId(wPlayerId);
        goldRef.setColor("gold");
        playerRefs.add(goldRef);

        MatchDocument.PlayerRef silverRef = new MatchDocument.PlayerRef();
        silverRef.setPlayerId(bPlayerId);
        silverRef.setColor("silver");
        playerRefs.add(silverRef);

        doc.setPlayers(playerRefs);

        MatchDocument.Event event = new MatchDocument.Event();
        event.setName(eventName);
        event.setIsRated(true);
        doc.setEvent(event);

        MatchDocument.GameType gt = new MatchDocument.GameType();
        gt.setName(timeControl);
        doc.setGameType(gt);
    }

    private void getOrCreatePlayer(Integer id, String username, String countryName, Integer rating) {
        Optional<PlayerDocument> existing = playerMongoRepository.findById(id);
        if (existing.isPresent()) {
            return;
        }

        UserDocument user = userMongoRepository.findByUsername(username).orElseGet(() -> {
            UserDocument newUser = new UserDocument();
            newUser.setUsername(username);
            newUser.setEmail(username + "@example.com");
            newUser.setPasswordHash("PLACEHOLDER");
            return userMongoRepository.save(newUser);
        });

        PlayerDocument player = new PlayerDocument();
        player.setId(id);
        player.setUser(user);
        player.setCountry(countryName);
        player.setRating(rating);
        player.setGamesPlayed(0);
        player.setRu(0);
        playerMongoRepository.save(player);
    }

    private MatchResponse toMatchResponse(MatchDocument doc) {
        PlayerSummary silver = null;
        PlayerSummary gold = null;

        if (doc.getPlayers() != null) {
            for (MatchDocument.PlayerRef ref : doc.getPlayers()) {
                PlayerSummary summary = getPlayerSummary(ref.getPlayerId());
                if ("silver".equalsIgnoreCase(ref.getColor())) {
                    silver = summary;
                } else if ("gold".equalsIgnoreCase(ref.getColor())) {
                    gold = summary;
                }
            }
        }

        EventSummary event = doc.getEvent() != null ?
                new EventSummary(doc.getEvent().getId(), doc.getEvent().getName()) : null;

        GameTypeSummary gameType = doc.getGameType() != null ?
                new GameTypeSummary(doc.getGameType().getId(), doc.getGameType().getName(),
                        doc.getGameType().getTimeIncrement(), doc.getGameType().getTimeReserve()) : null;

        return new MatchResponse(
                doc.getId(),
                doc.getTerminationType(),
                silver,
                gold,
                doc.getGoldRating(),
                doc.getSilverRating(),
                doc.getMatchResult(),
                event,
                gameType,
                doc.getTimestamp()
        );
    }

    private PlayerSummary getPlayerSummary(Integer playerId) {
        if (playerId == null) return null;
        return playerMongoRepository.findById(playerId)
                .map(p -> new PlayerSummary(p.getId(), p.getUser() != null ? p.getUser().getUsername() : "Unknown"))
                .orElse(new PlayerSummary(playerId, "Unknown"));
    }
}
