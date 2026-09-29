package com.sharedexpenses.service;

/** The request is valid but conflicts with the current state (e.g. duplicate name). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
