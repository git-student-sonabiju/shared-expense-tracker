package com.sharedexpenses.settlement;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DebtSimplifierTest {

    @Test
    void noTransfersWhenEveryoneIsSettled() {
        assertThat(DebtSimplifier.simplify(Map.of(1L, 0L, 2L, 0L))).isEmpty();
        assertThat(DebtSimplifier.simplify(Map.of())).isEmpty();
    }

    @Test
    void singleDebtorPaysSingleCreditor() {
        assertThat(DebtSimplifier.simplify(Map.of(1L, 500L, 2L, -500L)))
                .containsExactly(new Transfer(2, 1, 500));
    }

    @Test
    void oneCreditorManyDebtors() {
        List<Transfer> transfers = DebtSimplifier.simplify(Map.of(1L, 600L, 2L, -200L, 3L, -400L));
        assertThat(transfers).hasSize(2);
        assertSettles(Map.of(1L, 600L, 2L, -200L, 3L, -400L), transfers);
    }

    /**
     * Pure greedy (largest debtor pays largest creditor) needs 5 payments for these balances.
     * The optimum is 4: {+3, -3} settle between themselves and {-8, +6, -2, +4} need 3 payments.
     */
    @Test
    void findsFewerTransfersThanGreedyByUsingZeroSumSubgroups() {
        Map<Long, Long> balances = Map.of(1L, -800L, 2L, 600L, 3L, -200L, 4L, 300L, 5L, 400L, 6L, -300L);
        List<Transfer> transfers = DebtSimplifier.simplify(balances);
        assertThat(transfers).hasSize(4);
        assertThat(transfers).contains(new Transfer(6, 4, 300));
        assertSettles(balances, transfers);
    }

    @Test
    void randomBalancesAreFullySettledWithAtMostNMinusOneTransfers() {
        Random random = new Random(42);
        for (int run = 0; run < 200; run++) {
            int n = 2 + random.nextInt(9);
            Map<Long, Long> balances = new HashMap<>();
            long sum = 0;
            for (long id = 1; id < n; id++) {
                long value = random.nextInt(20001) - 10000;
                balances.put(id, value);
                sum += value;
            }
            balances.put((long) n, -sum);

            List<Transfer> transfers = DebtSimplifier.simplify(balances);
            long nonZero = balances.values().stream().filter(v -> v != 0).count();
            assertThat(transfers.size()).isLessThanOrEqualTo((int) Math.max(0, nonZero - 1));
            assertThat(transfers).allSatisfy(t -> assertThat(t.amountCents()).isPositive());
            assertSettles(balances, transfers);
        }
    }

    @Test
    void fallsBackToGreedyForVeryLargeGroups() {
        Map<Long, Long> balances = new HashMap<>();
        for (long id = 1; id <= 30; id++) {
            balances.put(id, id % 2 == 0 ? 100L : -100L);
        }
        List<Transfer> transfers = DebtSimplifier.simplify(balances);
        assertThat(transfers).hasSize(15);
        assertSettles(balances, transfers);
    }

    @Test
    void rejectsBalancesThatDoNotSumToZero() {
        assertThatThrownBy(() -> DebtSimplifier.simplify(Map.of(1L, 100L, 2L, -50L)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static void assertSettles(Map<Long, Long> balances, List<Transfer> transfers) {
        Map<Long, Long> remaining = new HashMap<>(balances);
        for (Transfer t : transfers) {
            remaining.merge(t.fromMemberId(), t.amountCents(), Long::sum);
            remaining.merge(t.toMemberId(), -t.amountCents(), Long::sum);
        }
        assertThat(remaining.values()).allMatch(v -> v == 0);
    }
}
