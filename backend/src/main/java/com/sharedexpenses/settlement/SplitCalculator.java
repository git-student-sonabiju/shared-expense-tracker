package com.sharedexpenses.settlement;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SplitCalculator {

    private SplitCalculator() {
    }

    /**
     * Splits {@code totalCents} equally among {@code memberIds}. When the amount does not
     * divide evenly, the leftover cents go one each to the first members in the given order,
     * so the shares always add up to exactly the total (e.g. 10.00 / 3 = 3.34, 3.33, 3.33).
     */
    public static Map<Long, Long> splitEqually(long totalCents, List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            throw new IllegalArgumentException("At least one member is required");
        }
        int count = memberIds.size();
        long base = totalCents / count;
        long remainder = totalCents % count;

        Map<Long, Long> shares = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            shares.put(memberIds.get(i), base + (i < remainder ? 1 : 0));
        }
        return shares;
    }
}
