package com.sharedexpenses.web;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/** Uniform error body: a human-readable message plus per-field details when relevant. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(Instant timestamp, int status, String error, String message, List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }
}
