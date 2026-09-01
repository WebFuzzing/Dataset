package com.example.arimaabackend.unittests;

import com.example.arimaabackend.model.sql.CountryEntity;
import com.example.arimaabackend.repository.sql.CountryJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CountryJpaRepositoryTest {

    @Autowired
    private CountryJpaRepository countryJpaRepository;

    @Test
    void shouldSaveAndLoadCountry() {
        var country = new CountryEntity();
        country.setId(1);
        country.setName("US");

        countryJpaRepository.save(country);

        var loaded = countryJpaRepository.findById(1);

        assertThat(loaded).isPresent();
        assertThat(loaded.get().getName()).isEqualTo("US");
    }
}