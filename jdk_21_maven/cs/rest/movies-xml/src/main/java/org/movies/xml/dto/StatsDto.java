package org.movies.xml.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Catalogue statistics: a stats root element with a total attribute and one genre
 * element per genre that actually has movies.
 */
@JacksonXmlRootElement(localName = "stats")
@JsonPropertyOrder({"total", "genres"})
public class StatsDto {

    @JacksonXmlProperty(isAttribute = true, localName = "total")
    private long total;

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "genre")
    private List<GenreStatsDto> genres = new ArrayList<>();

    public StatsDto() {
    }

    public StatsDto(long total, List<GenreStatsDto> genres) {
        this.total = total;
        setGenres(genres);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<GenreStatsDto> getGenres() {
        return genres;
    }

    public void setGenres(List<GenreStatsDto> genres) {
        this.genres = (genres == null) ? new ArrayList<>() : genres;
    }
}
