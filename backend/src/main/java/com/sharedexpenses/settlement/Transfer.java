package com.sharedexpenses.settlement;

/** A suggested payment: {@code fromMemberId} pays {@code amountCents} to {@code toMemberId}. */
public record Transfer(long fromMemberId, long toMemberId, long amountCents) {
}
