package com.sharedexpenses.domain;

public enum SplitType {
    /** Amount divided equally; leftover cents go to the first participants (by member id). */
    EQUAL,
    /** Each participant's share is given explicitly and must add up to the total. */
    EXACT
}
