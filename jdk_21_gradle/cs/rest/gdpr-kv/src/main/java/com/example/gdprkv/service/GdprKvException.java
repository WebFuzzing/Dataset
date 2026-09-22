package com.example.gdprkv.service;

import lombok.Getter;

public class GdprKvException extends RuntimeException {

    public enum Code {
        INVALID_PURPOSE,
        SUBJECT_ALREADY_EXISTS,
        SUBJECT_NOT_FOUND,
        RECORD_NOT_FOUND,
        UNKNOWN
    }

    @Getter
    private final Code code;

    private GdprKvException(Code code, String message) {
        super(message);
        this.code = code;
    }

    public static GdprKvException invalidPurpose(String purpose) {
        return new GdprKvException(Code.INVALID_PURPOSE,
                "Purpose " + purpose + " is not configured");
    }

    public static GdprKvException subjectNotFound(String subjectId) {
        return new GdprKvException(Code.SUBJECT_NOT_FOUND,
                "Subject " + subjectId + " does not exist");
    }

    public static GdprKvException subjectAlreadyExists(String subjectId) {
        return new GdprKvException(Code.SUBJECT_ALREADY_EXISTS,
                "Subject " + subjectId + " already exists");
    }

    public static GdprKvException recordNotFound(String subjectId, String recordKey) {
        return new GdprKvException(Code.RECORD_NOT_FOUND,
                "Record " + recordKey + " for subject " + subjectId + " does not exist");
    }
}
