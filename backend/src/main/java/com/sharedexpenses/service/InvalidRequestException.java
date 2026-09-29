package com.sharedexpenses.service;

/** Input that is well-formed but violates a business rule. Mapped to HTTP 400. */
public class InvalidRequestException extends RuntimeException {

    private final String field;

    public InvalidRequestException(String message) {
        super(message);
        this.field = null;
    }

    public InvalidRequestException(String field, String message) {
        super(field + " " + message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
