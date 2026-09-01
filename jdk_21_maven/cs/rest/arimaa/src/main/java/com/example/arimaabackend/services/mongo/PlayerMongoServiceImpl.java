package com.example.arimaabackend.services.mongo;

import com.example.arimaabackend.dto.PlayerCreateRequest;
import com.example.arimaabackend.dto.PlayerResponse;
import com.example.arimaabackend.dto.PlayerUpdateRequest;
import com.example.arimaabackend.model.mongo.PlayerDocument;
import com.example.arimaabackend.model.mongo.UserDocument;
import com.example.arimaabackend.repository.mongo.MatchMongoRepository;
import com.example.arimaabackend.repository.mongo.PlayerMongoRepository;
import com.example.arimaabackend.repository.mongo.UserMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlayerMongoServiceImpl implements PlayerMongoService {

    private final PlayerMongoRepository playerMongoRepository;
    private final UserMongoRepository userMongoRepository;
    private final MatchMongoRepository matchMongoRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerMongoServiceImpl(PlayerMongoRepository playerMongoRepository,
                                  UserMongoRepository userMongoRepository,
                                  MatchMongoRepository matchMongoRepository,
                                  PasswordEncoder passwordEncoder) {
        this.playerMongoRepository = playerMongoRepository;
        this.userMongoRepository = userMongoRepository;
        this.matchMongoRepository = matchMongoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PlayerResponse create(PlayerCreateRequest request) {
        UserDocument user = userMongoRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found in Mongo".formatted(request.userId())));

        if (playerMongoRepository.findById(user.getId().intValue()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Player for user %d already exists in Mongo".formatted(user.getId()));
        }

        PlayerDocument doc = new PlayerDocument();
        doc.setId(user.getId().intValue());
        doc.setUser(user);
        doc.setRating(request.rating() != null ? request.rating() : 1200);
        doc.setRu(request.ru() != null ? request.ru() : 0);
        doc.setGamesPlayed(request.gamesPlayed() != null ? request.gamesPlayed() : 0);
        // Map country name to country field in Mongo if available, or use countryId
        doc.setCountry(request.country() != null ? request.country() : (request.countryId() != null ? request.countryId().toString() : null));

        doc = playerMongoRepository.save(doc);
        return toResponse(doc);
    }

    @Override
    public PlayerResponse update(Integer id, PlayerUpdateRequest request) {
        PlayerDocument doc = playerMongoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player %d not found in Mongo".formatted(id)));

        doc.setRating(request.rating());
        doc.setRu(request.ru());
        doc.setGamesPlayed(request.gamesPlayed());
        doc.setCountry(request.countryId() != null ? request.countryId().toString() : null);

        if (request.userUpdate() != null) {
            UserDocument user = doc.getUser();
            if (user != null) {
                user.setUsername(request.userUpdate().username());
                user.setEmail(request.userUpdate().email());
                if (request.userUpdate().password() != null && !request.userUpdate().password().isBlank()) {
                    user.setPasswordHash(passwordEncoder.encode(request.userUpdate().password()));
                }
                user.setUpdatedAt(Instant.now());
                userMongoRepository.save(user);
            }
        }

        doc = playerMongoRepository.save(doc);
        return toResponse(doc);
    }

    @Override
    public PlayerResponse getByUsername(String username) {
        return playerMongoRepository.findByUser_Username(username)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player with username '%s' not found in Mongo".formatted(username)));
    }

    @Override
    public PlayerResponse getById(Integer id) {
        return playerMongoRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player %d not found in Mongo".formatted(id)));
    }

    @Override
    public List<PlayerResponse> getAll() {
        return playerMongoRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlayerResponse> getByCountryName(String countryName) {
        return playerMongoRepository.findByCountry(countryName).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(Integer id) {
        if (!playerMongoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player %d not found in Mongo".formatted(id));
        }
        
        boolean hasMatches = !matchMongoRepository.findByPlayerId(id).isEmpty();
        
        if (hasMatches) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete player with matches in Mongo");
        }
        
        playerMongoRepository.deleteById(id);
    }

    private PlayerResponse toResponse(PlayerDocument doc) {
        UserDocument user = doc.getUser();
        Integer countryId = null;
        try {
            if (doc.getCountry() != null) {
                countryId = Integer.parseInt(doc.getCountry());
            }
        } catch (NumberFormatException ignored) {}

        return new PlayerResponse(
                doc.getId(),
                user != null ? user.getUsername() : null,
                user != null ? user.getEmail() : null,
                doc.getRating(),
                doc.getRu(),
                doc.getGamesPlayed(),
                user != null ? user.getCreatedAt() : null,
                countryId
        );
    }
}
