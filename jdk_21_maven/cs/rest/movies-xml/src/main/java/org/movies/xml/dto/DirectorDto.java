package org.movies.xml.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Director of a movie: a nested element whose nationality travels as an attribute.
 */
@JacksonXmlRootElement(localName = "director")
@JsonPropertyOrder({"nationality", "name"})
public class DirectorDto {

    @JacksonXmlProperty(isAttribute = true, localName = "nationality")
    @Pattern(regexp = "^[A-Z]{2}$")
    private String nationality;

    @JacksonXmlProperty(localName = "name")
    @NotBlank
    @Size(min = 1, max = 120)
    private String name;

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
