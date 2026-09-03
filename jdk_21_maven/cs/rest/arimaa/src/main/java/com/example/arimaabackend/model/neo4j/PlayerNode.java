package com.example.arimaabackend.model.neo4j;

import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Player")
public class PlayerNode {

    @Id
    private Integer id;

    private Integer rating;
    private Integer ru;
    private Integer gamesPlayed;

    @Relationship(type = "HAS_USER")
    private UserNode user;

    @Relationship(type = "IN_COUNTRY")
    private CountryNode country;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public Integer getRu() {
        return ru;
    }

    public void setRu(Integer ru) {
        this.ru = ru;
    }

    public Integer getGamesPlayed() {
        return gamesPlayed;
    }

    public void setGamesPlayed(Integer gamesPlayed) {
        this.gamesPlayed = gamesPlayed;
    }

    public UserNode getUser() {
        return user;
    }

    public void setUser(UserNode user) {
        this.user = user;
    }

    public CountryNode getCountry() {
        return country;
    }

    public void setCountry(CountryNode country) {
        this.country = country;
    }
}

