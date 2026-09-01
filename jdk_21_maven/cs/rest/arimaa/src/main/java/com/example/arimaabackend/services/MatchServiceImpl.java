package com.example.arimaabackend.services;

import com.example.arimaabackend.dto.*;
import com.example.arimaabackend.model.sql.*;
import com.example.arimaabackend.repository.sql.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;

@Service
public class MatchServiceImpl implements MatchService {

    private final MatchJpaRepository matchRepository;
    private final PlayerJpaRepository playerRepository;
    private final UserJpaRepository userRepository;
    private final EventJpaRepository eventRepository;
    private final GameTypeJpaRepository gameTypeRepository;
    private final CountryJpaRepository countryRepository;
    private final MoveJpaRepository moveRepository;
    private final PositionJpaRepository positionRepository;
    public MatchServiceImpl(
            MatchJpaRepository matchRepository,
            PlayerJpaRepository playerRepository,
            UserJpaRepository userRepository,
            EventJpaRepository eventRepository,
            GameTypeJpaRepository gameTypeRepository,
            CountryJpaRepository countryRepository,
            MoveJpaRepository moveRepository,
            PositionJpaRepository positionRepository) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.gameTypeRepository = gameTypeRepository;
        this.countryRepository = countryRepository;
        this.moveRepository = moveRepository;
        this.positionRepository = positionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MatchResponse> getMatch(Integer id) {
        return matchRepository.findById(id).map(this::toMatchResponse);
    }

    @Override
    @Transactional
    public MatchResponse createMatch(String matchData) {
        String[] parts = splitAndValidateMatchData(matchData);
        Integer id = Integer.parseInt(parts[0]);

        MatchEntity match = new MatchEntity();
        match.setId(id);
        populateMatchFromParts(match, parts);

        return toMatchResponse(matchRepository.save(match));
    }

    @Override
    @Transactional
    public MatchResponse updateMatch(Integer id, String matchData) {
        MatchEntity match = matchRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Match not found"));

        String[] parts = splitAndValidateMatchData(matchData);
        populateMatchFromParts(match, parts);

        return toMatchResponse(matchRepository.save(match));
    }

    private String[] splitAndValidateMatchData(String matchData) {
        String[] parts = matchData.split("\t");
        if (parts.length < 23) {
            throw new IllegalArgumentException("Invalid match data format");
        }
        return parts;
    }

    private void populateMatchFromParts(MatchEntity match, String[] parts) {
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

        PlayerEntity goldPlayer = getOrCreatePlayer(wPlayerId, wUsername, wCountryName, wRating);
        PlayerEntity silverPlayer = getOrCreatePlayer(bPlayerId, bUsername, bCountryName, bRating);
        EventEntity event = getOrCreateEvent(eventName);
        GameTypeEntity gameType = getOrCreateGameType(timeControl);

        match.setGoldPlayer(goldPlayer);
        match.setSilverPlayer(silverPlayer);
        match.setGoldRating(wRating);
        match.setSilverRating(bRating);
        match.setEvent(event);
        match.setGameType(gameType);
        match.setTimestamp(Instant.ofEpochSecond(startTs));
        match.setMatchResult(result);
        match.setTerminationType(termination);
    }

    private MatchResponse toMatchResponse(MatchEntity match) {
        PlayerSummary silverPlayer = null;
        if (match.getSilverPlayer() != null) {
            String username = match.getSilverPlayer().getUser() != null ? match.getSilverPlayer().getUser().getUsername() : null;
            silverPlayer = new PlayerSummary(match.getSilverPlayer().getId(), username);
        }

        PlayerSummary goldPlayer = null;
        if (match.getGoldPlayer() != null) {
            String username = match.getGoldPlayer().getUser() != null ? match.getGoldPlayer().getUser().getUsername() : null;
            goldPlayer = new PlayerSummary(match.getGoldPlayer().getId(), username);
        }

        EventSummary event = null;
        if (match.getEvent() != null) {
            event = new EventSummary(match.getEvent().getId(), match.getEvent().getName());
        }

        GameTypeSummary gameType = null;
        if (match.getGameType() != null) {
            gameType = new GameTypeSummary(
                    match.getGameType().getId(),
                    match.getGameType().getName(),
                    match.getGameType().getTimeIncrement(),
                    match.getGameType().getTimeReserve());
        }

        return new MatchResponse(
                match.getId(),
                match.getTerminationType(),
                silverPlayer,
                goldPlayer,
                match.getGoldRating(),
                match.getSilverRating(),
                match.getMatchResult(),
                event,
                gameType,
                match.getTimestamp()
        );
    }

    @Override
    @Transactional
    public void deleteMatch(Integer id) {
        if (!matchRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Match with ID " + id + " not found");
        }
        positionRepository.deleteUnusedByMatchId(id);
        moveRepository.deleteByMatch_Id(id);
        matchRepository.deleteById(id);
    }

    private PlayerEntity getOrCreatePlayer(Integer id, String username, String countryName, Integer rating) {
        // First, check if the player already exists
        Optional<PlayerEntity> existingPlayerOpt = playerRepository.findById(id);

        if (existingPlayerOpt.isPresent()) {
            PlayerEntity existingPlayer = existingPlayerOpt.get();
            String existingUsername = existingPlayer.getUser() != null
                    ? existingPlayer.getUser().getUsername()
                    : null;

            // If the username doesn't match, this is a data inconsistency → fail fast
            if (!username.equals(existingUsername)) {
                throw new IllegalArgumentException(
                        String.format("Player with ID %d exists but has different username. " +
                                        "Expected: %s, Found: %s",
                                id, username, existingUsername));
            }

            // Player exists and username matches → return it
            return existingPlayer;
        }

        // Player doesn't exist → create new one (with user and country if needed)
        UserEntity user = userRepository.findByUsername(username).orElseGet(() -> {
            UserEntity newUser = new UserEntity();
            newUser.setUsername(username);
            newUser.setEmail(username + "@example.com");
            newUser.setPasswordHash("PLACEHOLDER");
            return userRepository.save(newUser);
        });

        CountryEntity country = countryRepository.findByName(countryName).orElseGet(() -> {
            CountryEntity newCountry = new CountryEntity();
            newCountry.setId(countryName.hashCode()); // Consider proper ID strategy later
            newCountry.setName(countryName);
            return countryRepository.save(newCountry);
        });

        PlayerEntity player = new PlayerEntity();
        player.setId(id);
        player.setUser(user);
        player.setCountry(country);
        player.setRating(rating);
        player.setGamesPlayed(0);
        player.setRu(0);

        return playerRepository.save(player);
    }

    private EventEntity getOrCreateEvent(String name) {
        return eventRepository.findByName(name).orElseGet(() -> {
            EventEntity event = new EventEntity();
            event.setId(name.hashCode());
            event.setName(name);
            event.setRated(true);
            return eventRepository.save(event);
        });
    }

    private GameTypeEntity getOrCreateGameType(String name) {
        return gameTypeRepository.findByName(name).orElseGet(() -> {
            GameTypeEntity gameType = new GameTypeEntity();
            gameType.setId(name.hashCode());
            gameType.setName(name);
            gameType.setTimeIncrement(0);
            gameType.setTimeReserve(0);
            return gameTypeRepository.save(gameType);
        });
    }
}
