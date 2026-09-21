package org.movies.xml.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection wrapper: a movies root element holding movie elements.
 */
@JacksonXmlRootElement(localName = "movies")
public class MoviesDto {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "movie")
    private List<MovieDto> movies = new ArrayList<>();

    public MoviesDto() {
    }

    public MoviesDto(List<MovieDto> movies) {
        setMovies(movies);
    }

    public List<MovieDto> getMovies() {
        return movies;
    }

    public void setMovies(List<MovieDto> movies) {
        this.movies = (movies == null) ? new ArrayList<>() : movies;
    }
}
