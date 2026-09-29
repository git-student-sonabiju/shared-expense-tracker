package com.sharedexpenses.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Conversion between the API representation (decimal with 2 places) and the storage
 * representation (whole cents as a long). All arithmetic happens on cents, so there is
 * no floating point rounding and totals are exact.
 */
public final class Money {

    private Money() {
    }

    /** Converts a decimal amount to cents; rejects values with more than two decimal places. */
    public static long toCents(BigDecimal amount, String field) {
        if (amount == null) {
            throw new InvalidRequestException(field, "is required");
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 2) {
            throw new InvalidRequestException(field, "must have at most 2 decimal places");
        }
        try {
            return normalized.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new InvalidRequestException(field, "is too large");
        }
    }

    public static BigDecimal fromCents(long cents) {
        return BigDecimal.valueOf(cents, 2).setScale(2, RoundingMode.UNNECESSARY);
    }
}
