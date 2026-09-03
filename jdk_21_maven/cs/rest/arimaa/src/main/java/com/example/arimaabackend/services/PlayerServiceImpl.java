package com.example.arimaabackend.services;

import com.example.arimaabackend.dto.PlayerCreateRequest;
import com.example.arimaabackend.dto.PlayerResponse;
import com.example.arimaabackend.dto.PlayerUpdateRequest;
import com.example.arimaabackend.model.sql.CountryEntity;
import com.example.arimaabackend.model.sql.PlayerEntity;
import com.example.arimaabackend.model.sql.UserEntity;
import com.example.arimaabackend.repository.sql.CountryJpaRepository;
import com.example.arimaabackend.repository.sql.MatchJpaRepository;
import com.example.arimaabackend.repository.sql.PlayerJpaRepository;
import com.example.arimaabackend.repository.sql.UserJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class PlayerServiceImpl implements PlayerService {

    private final PlayerJpaRepository playerJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final CountryJpaRepository countryJpaRepository;
    private final MatchJpaRepository matchJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public PlayerServiceImpl(PlayerJpaRepository playerJpaRepository,
                             UserJpaRepository userJpaRepository,
                             CountryJpaRepository countryJpaRepository,
                             MatchJpaRepository matchJpaRepository,
                             PasswordEncoder passwordEncoder) {
        this.playerJpaRepository = playerJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.countryJpaRepository = countryJpaRepository;
        this.matchJpaRepository = matchJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public PlayerResponse create(PlayerCreateRequest request) {
        UserEntity user = userJpaRepository.findById(request.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User %d not found".formatted(request.userId())));

        if (playerJpaRepository.findByUser_Id(user.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Player for user %d already exists".formatted(user.getId()));
        }

        CountryEntity country = null;
        if (request.countryId() != null) {
            country = countryJpaRepository.findById(request.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country %d not found".formatted(request.countryId())));
        }

        PlayerEntity entity = new PlayerEntity();
        entity.setUser(user);
        entity.setCountry(country);
        entity.setRating(request.rating() != null ? request.rating() : 1200);
        entity.setRu(request.ru() != null ? request.ru() : 0);
        entity.setGamesPlayed(request.gamesPlayed() != null ? request.gamesPlayed() : 0);
        entity.setId(user.getId().intValue());
        entity = playerJpaRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    public PlayerResponse update(Integer id, PlayerUpdateRequest request) {
        PlayerEntity entity = playerJpaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player %d not found".formatted(id)));

        CountryEntity country = null;
        if (request.countryId() != null) {
            country = countryJpaRepository.findById(request.countryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Country %d not found".formatted(request.countryId())));
        }

        entity.setRating(request.rating());
        entity.setRu(request.ru());
        entity.setGamesPlayed(request.gamesPlayed());
        entity.setCountry(country);

        if (request.userUpdate() != null) {
            UserEntity user = entity.getUser();
            user.setUsername(request.userUpdate().username());
            user.setEmail(request.userUpdate().email());
            if (request.userUpdate().password() != null && !request.userUpdate().password().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(request.userUpdate().password()));
            }
            userJpaRepository.save(user);
        }

        entity = playerJpaRepository.save(entity);
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getByUsername(String username) {

        UserEntity user = userJpaRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User '%s' not found".formatted(username)));

        PlayerResponse player = playerJpaRepository
                .findByUser_Id(user.getId()).map(PlayerServiceImpl::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player '%s' not found".formatted(username)));

        return new PlayerResponse(
                player.id(),
                user.getUsername(),
                user.getEmail(),
                player.rating(),
                player.ru(),
                player.gamesPlayed(),
                player.createTime(),
                player.countryId()
        );
    }


    @Override
    @Transactional(readOnly = true)
    public PlayerResponse getById(Integer id) {
        return playerJpaRepository.findById(id)
                .map(PlayerServiceImpl::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player %d not found".formatted(id)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getAll() {
        return playerJpaRepository.findAll().stream()
                .map(PlayerServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerResponse> getByCountryName(String countryName) {
        return playerJpaRepository.findByCountryName(countryName).stream()
                .map(PlayerServiceImpl::toResponse)
                .toList();
    }

    @Override
    public void deleteById(Integer id) {
        if (!playerJpaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player %d not found".formatted(id));
        }

        boolean hasMatches = !matchJpaRepository.findByGoldPlayer_Id(id).isEmpty() ||
                             !matchJpaRepository.findBySilverPlayer_Id(id).isEmpty();
        
        if (hasMatches) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete player with matches");
        }

        playerJpaRepository.deleteById(id);
    }

    private static PlayerResponse toResponse(PlayerEntity entity) {
        UserEntity user = entity.getUser();
        return new PlayerResponse(
                entity.getId(),
                user != null ? user.getUsername() : "",
                user != null ? user.getEmail() : "",
                entity.getRating(),
                entity.getRu(),
                entity.getGamesPlayed(),
                null,
                entity.getCountry() != null ? entity.getCountry().getId() : null
        );
    }
}
