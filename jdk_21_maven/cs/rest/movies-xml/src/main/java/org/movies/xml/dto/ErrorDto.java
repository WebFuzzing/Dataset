package org.movies.xml.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Every error this API produces: an error root element with the status as an
 * attribute and a message element.
 */
@JacksonXmlRootElement(localName = "error")
@JsonPropertyOrder({"status", "message"})
public class ErrorDto {

    @JacksonXmlProperty(isAttribute = true, localName = "status")
    private int status;

    @JacksonXmlProperty(localName = "message")
    private String message;

    public ErrorDto() {
    }

    public ErrorDto(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
