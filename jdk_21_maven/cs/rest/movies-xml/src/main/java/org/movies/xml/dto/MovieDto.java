package org.movies.xml.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.movies.xml.domain.Genre;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The one and only accepted representation of a movie. Root element is "movie",
 * id and year are attributes, the director is a nested element with a nationality
 * attribute, and the cast is a wrapped list of actor elements.
 */
@JacksonXmlRootElement(localName = "movie")
@JsonPropertyOrder({"id", "year", "title", "genre", "rating", "director", "cast"})
public class MovieDto {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    private Long id;

    @JacksonXmlProperty(isAttribute = true, localName = "year")
    @NotNull
    @Min(1888)
    @Max(2100)
    private Integer year;

    @JacksonXmlProperty(localName = "title")
    @NotBlank
    @Size(min = 1, max = 200)
    private String title;

    @JacksonXmlProperty(localName = "genre")
    @NotNull
    private Genre genre;

    @JacksonXmlProperty(localName = "rating")
    @DecimalMin("0.0")
    @DecimalMax("10.0")
    private Double rating;

    @JacksonXmlProperty(localName = "director")
    @NotNull
    @Valid
    private DirectorDto director;

    @JacksonXmlElementWrapper(localName = "cast")
    @JacksonXmlProperty(localName = "actor")
    @Size(max = 10)
    @Valid
    private List<ActorDto> cast;

    /**
     * Names literally present in the received document, filled in by the strict XML
     * reader. It is what lets PATCH tell an absent field apart from one sent empty,
     * and it is never part of the wire format.
     */
    @JsonIgnore
    @Schema(hidden = true)
    private Set<String> presentNames = Collections.emptySet();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public DirectorDto getDirector() {
        return director;
    }

    public void setDirector(DirectorDto director) {
        this.director = director;
    }

    public List<ActorDto> getCast() {
        return cast;
    }

    public void setCast(List<ActorDto> cast) {
        this.cast = cast;
    }

    @JsonIgnore
    public Set<String> getPresentNames() {
        return presentNames;
    }

    public void setPresentNames(Set<String> presentNames) {
        this.presentNames = (presentNames == null) ? Collections.emptySet() : presentNames;
    }

    @JsonIgnore
    public boolean isPresent(String name) {
        return presentNames.contains(name);
    }

    /**
     * The cast exactly as received: an empty cast element yields an empty list,
     * an absent one yields null.
     */
    @JsonIgnore
    public List<ActorDto> getCastAsReceived() {
        if (!isPresent("cast")) {
            return null;
        }
        return (cast == null) ? new ArrayList<>() : cast;
    }
}
