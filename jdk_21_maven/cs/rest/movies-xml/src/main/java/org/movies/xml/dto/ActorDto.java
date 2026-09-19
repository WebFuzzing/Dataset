package org.movies.xml.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A single entry of a movie cast: an actor element carrying billing as an attribute.
 */
@JacksonXmlRootElement(localName = "actor")
@JsonPropertyOrder({"billing", "name"})
public class ActorDto {

    @JacksonXmlProperty(isAttribute = true, localName = "billing")
    @Min(1)
    @Max(99)
    private Integer billing;

    @JacksonXmlProperty(localName = "name")
    @NotBlank
    @Size(min = 1, max = 120)
    private String name;

    public Integer getBilling() {
        return billing;
    }

    public void setBilling(Integer billing) {
        this.billing = billing;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
