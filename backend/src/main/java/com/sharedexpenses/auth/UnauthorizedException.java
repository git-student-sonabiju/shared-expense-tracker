package com.sharedexpenses.auth;

/** Missing, invalid or expired credentials. Mapped to HTTP 401. */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
