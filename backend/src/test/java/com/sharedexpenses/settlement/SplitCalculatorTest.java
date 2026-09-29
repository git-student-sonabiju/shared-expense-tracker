package com.sharedexpenses.settlement;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SplitCalculatorTest {

    @Test
    void splitsEvenlyWhenDivisible() {
        assertThat(SplitCalculator.splitEqually(900, List.of(1L, 2L, 3L)))
                .containsExactly(Map.entry(1L, 300L), Map.entry(2L, 300L), Map.entry(3L, 300L));
    }

    @Test
    void distributesLeftoverCentsToFirstMembers() {
        Map<Long, Long> shares = SplitCalculator.splitEqually(1000, List.of(1L, 2L, 3L));
        assertThat(shares).containsExactly(Map.entry(1L, 334L), Map.entry(2L, 333L), Map.entry(3L, 333L));
        assertThat(shares.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(1000);
    }

    @Test
    void sharesAlwaysSumToTotal() {
        for (long total = 1; total < 500; total += 7) {
            for (int n = 1; n <= 7 && n <= total; n++) {
                List<Long> ids = java.util.stream.LongStream.rangeClosed(1, n).boxed().toList();
                long sum = SplitCalculator.splitEqually(total, ids).values().stream().mapToLong(Long::longValue).sum();
                assertThat(sum).isEqualTo(total);
            }
        }
    }

    @Test
    void rejectsEmptyMemberList() {
        assertThatThrownBy(() -> SplitCalculator.splitEqually(100, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
