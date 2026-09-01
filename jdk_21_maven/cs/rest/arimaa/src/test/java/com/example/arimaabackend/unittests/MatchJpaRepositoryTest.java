package com.example.arimaabackend.unittests;

import com.example.arimaabackend.model.sql.*;
import com.example.arimaabackend.repository.sql.CountryJpaRepository;
import com.example.arimaabackend.repository.sql.EventJpaRepository;
import com.example.arimaabackend.repository.sql.GameTypeJpaRepository;
import com.example.arimaabackend.repository.sql.MatchJpaRepository;
import com.example.arimaabackend.repository.sql.PlayerJpaRepository;
import com.example.arimaabackend.repository.sql.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class MatchJpaRepositoryTest {

    @Autowired
    private MatchJpaRepository matchJpaRepository;

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private CountryJpaRepository countryJpaRepository;

    @Autowired
    private EventJpaRepository eventJpaRepository;

    @Autowired
    private GameTypeJpaRepository gameTypeJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void shouldSaveAndLoadMatchAndFindByPlayerIds() {
        var countryUs = new CountryEntity();
        countryUs.setId(1);
        countryUs.setName("US");
        countryJpaRepository.save(countryUs);

        var countryAu = new CountryEntity();
        countryAu.setId(2);
        countryAu.setName("AU");
        countryJpaRepository.save(countryAu);

        var silverUser = new UserEntity();
        silverUser.setUsername("Matthias");
        silverUser.setEmail("matthias@example.com");
        silverUser.setPasswordHash("password");

        var silver = new PlayerEntity();
        silver.setId(4803);
        silver.setUser(silverUser);
        silver.setCountry(countryUs);
        userJpaRepository.save(silverUser);
        playerJpaRepository.save(silver);

        var goldUser = new UserEntity();
        goldUser.setUsername("bot_GnoBot2006P1");
        goldUser.setEmail("bot@example.com");
        goldUser.setPasswordHash("password");

        var gold = new PlayerEntity();
        gold.setId(4613);
        gold.setUser(goldUser);
        gold.setCountry(countryAu);
        userJpaRepository.save(goldUser);
        playerJpaRepository.save(gold);

        var event = new EventEntity();
        event.setId(1);
        event.setName("Casual game");
        event.setRated(false);
        eventJpaRepository.save(event);

        var gameType = new GameTypeEntity();
        gameType.setId(1);
        gameType.setName("Over the Net");
        gameType.setTimeIncrement(2);
        gameType.setTimeReserve(2);
        gameTypeJpaRepository.save(gameType);

        var match = new MatchEntity();
        match.setId(27557);
        match.setTerminationType("test");
        match.setSilverPlayer(silver);
        match.setGoldPlayer(gold);
        match.setMatchResult("unknown");
        match.setEvent(event);
        match.setGameType(gameType);

        matchJpaRepository.save(match);

        var loaded = matchJpaRepository.findById(27557);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getSilverPlayer().getUser().getUsername()).isEqualTo("Matthias");
        assertThat(loaded.get().getGoldPlayer().getUser().getUsername()).isEqualTo("bot_GnoBot2006P1");

        var bySilver = matchJpaRepository.findBySilverPlayer_Id(4803);
        var byGold = matchJpaRepository.findByGoldPlayer_Id(4613);
        var byEvent = matchJpaRepository.findByEvent_Id(1);
        var byGameType = matchJpaRepository.findByGameType_Id(1);

        assertThat(bySilver).hasSize(1);
        assertThat(byGold).hasSize(1);
        assertThat(byEvent).hasSize(1);
        assertThat(byGameType).hasSize(1);
    }
}