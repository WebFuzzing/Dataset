package com.example.arimaabackend.unittests;

import com.example.arimaabackend.model.sql.CountryEntity;
import com.example.arimaabackend.model.sql.PlayerEntity;
import com.example.arimaabackend.model.sql.UserEntity;
import com.example.arimaabackend.repository.sql.CountryJpaRepository;
import com.example.arimaabackend.repository.sql.PlayerJpaRepository;
import com.example.arimaabackend.repository.sql.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PlayerJpaRepositoryTest {

    @Autowired
    private PlayerJpaRepository playerJpaRepository;

    @Autowired
    private CountryJpaRepository countryJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Test
    void shouldSaveAndLoadPlayerByUsername() {
        var country = new CountryEntity();
        country.setId(1);
        country.setName("US");
        countryJpaRepository.save(country);

        var playerUser = new UserEntity();
        playerUser.setUsername("Matthias");
        playerUser.setEmail("matthias@example.com");
        playerUser.setPasswordHash("hashed_password");
        userJpaRepository.save(playerUser);
        
        var player = new PlayerEntity();
        player.setId(4803);
        player.setUser(playerUser);
        player.setCountry(country);

        playerJpaRepository.save(player);

        var loaded = playerJpaRepository.findByUser_Username("Matthias");

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(4803);
        assertThat(loaded.get().getUser().getUsername()).isEqualTo("Matthias");
        assertThat(loaded.get().getCountry().getName()).isEqualTo("US");
    }

    @Test
    void shouldFindPlayersByCountryName() {
        var countryUs = new CountryEntity();
        countryUs.setId(1);
        countryUs.setName("US");
        countryJpaRepository.save(countryUs);

        var countryAu = new CountryEntity();
        countryAu.setId(2);
        countryAu.setName("AU");
        countryJpaRepository.save(countryAu);

        var player1User = new UserEntity();
        player1User.setUsername("Matthias");
        player1User.setEmail("matthias@example.com");
        player1User.setPasswordHash("hashed_password");
        userJpaRepository.save(player1User);
        
        var player1 = new PlayerEntity();
        player1.setId(4803);
        player1.setUser(player1User);
        player1.setCountry(countryUs);
        playerJpaRepository.save(player1);

        var player2User = new UserEntity();
        player2User.setUsername("bot_GnoBot2006P1");
        player2User.setEmail("bot@example.com");
        player2User.setPasswordHash("hashed_password");
        userJpaRepository.save(player2User);
        
        var player2 = new PlayerEntity();
        player2.setId(4613);
        player2.setUser(player2User);
        player2.setCountry(countryAu);
        playerJpaRepository.save(player2);

        var usPlayers = playerJpaRepository.findByCountryName("US");

        assertThat(usPlayers).hasSize(1);
        assertThat(usPlayers.get(0).getUser().getUsername()).isEqualTo("Matthias");
    }
}