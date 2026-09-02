package com.example.joinUs.dto;

public class ResponseMessage {

    public String status;
    public String message;

    public ResponseMessage(String status, String message) {
        this.status = status;
        this.message = message;
    }
}
