package org.movies.xml.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Per-genre figures: name and count as attributes, averageRating as a nested element
 * that is omitted when no movie of the genre carries a rating.
 */
@JacksonXmlRootElement(localName = "genre")
@JsonPropertyOrder({"name", "count", "averageRating"})
public class GenreStatsDto {

    @JacksonXmlProperty(isAttribute = true, localName = "name")
    private String name;

    @JacksonXmlProperty(isAttribute = true, localName = "count")
    private long count;

    @JacksonXmlProperty(localName = "averageRating")
    private Double averageRating;

    public GenreStatsDto() {
    }

    public GenreStatsDto(String name, long count, Double averageRating) {
        this.name = name;
        this.count = count;
        this.averageRating = averageRating;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }
}
