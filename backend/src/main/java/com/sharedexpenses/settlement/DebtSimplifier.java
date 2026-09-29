package com.sharedexpenses.settlement;

import java.util.*;

/**
 * Computes a set of payments that settles every balance using the minimum number of transactions.
 *
 * <p>Key observation: if the non-zero balances can be split into k disjoint subgroups that each
 * sum to zero, each subgroup of size s can be settled internally with s - 1 payments, giving
 * n - k payments in total. Every settlement plan induces such a partition, so the minimum number
 * of payments is n - (maximum number of zero-sum subgroups). Finding that maximum is NP-hard in
 * general, so we solve it exactly with a DP over subsets (O(n * 2^n)) for up to
 * {@value #EXACT_LIMIT} non-zero members, which covers any realistic group. Above that we fall
 * back to the greedy "largest debtor pays largest creditor" heuristic, which is guaranteed to use
 * at most n - 1 payments but is not always optimal.
 *
 * <p>Balances are in cents: positive means the member is owed money, negative means they owe.
 */
public final class DebtSimplifier {

    static final int EXACT_LIMIT = 20;

    private DebtSimplifier() {
    }

    public static List<Transfer> simplify(Map<Long, Long> balances) {
        List<long[]> nonZero = new ArrayList<>(); // [memberId, balance]
        long total = 0;
        for (Map.Entry<Long, Long> entry : new TreeMap<>(balances).entrySet()) {
            total += entry.getValue();
            if (entry.getValue() != 0) {
                nonZero.add(new long[]{entry.getKey(), entry.getValue()});
            }
        }
        if (total != 0) {
            throw new IllegalArgumentException("Balances must sum to zero but sum to " + total);
        }

        List<List<long[]>> subgroups = nonZero.size() <= EXACT_LIMIT
                ? partitionIntoMaxZeroSumGroups(nonZero)
                : List.of(nonZero);

        List<Transfer> transfers = new ArrayList<>();
        for (List<long[]> subgroup : subgroups) {
            settleGreedily(subgroup, transfers);
        }
        return transfers;
    }

    /**
     * dp[mask] = the maximum number of zero-sum groups the members in {@code mask} can be split
     * into, if they are removed one at a time. Walking back from the full set and cutting at every
     * zero-sum mask recovers the groups themselves.
     */
    private static List<List<long[]>> partitionIntoMaxZeroSumGroups(List<long[]> members) {
        int n = members.size();
        if (n == 0) {
            return List.of();
        }
        int full = (1 << n) - 1;
        long[] sum = new long[full + 1];
        int[] dp = new int[full + 1];
        int[] removed = new int[full + 1];

        for (int mask = 1; mask <= full; mask++) {
            int lowest = Integer.numberOfTrailingZeros(mask);
            sum[mask] = sum[mask & (mask - 1)] + members.get(lowest)[1];

            int best = -1;
            for (int rest = mask; rest != 0; rest &= rest - 1) {
                int bit = Integer.numberOfTrailingZeros(rest);
                int candidate = dp[mask ^ (1 << bit)];
                if (candidate > best) {
                    best = candidate;
                    removed[mask] = bit;
                }
            }
            dp[mask] = best + (sum[mask] == 0 ? 1 : 0);
        }

        List<List<long[]>> groups = new ArrayList<>();
        int lastCut = full;
        int mask = full;
        while (mask != 0) {
            mask ^= 1 << removed[mask];
            if (sum[mask] == 0) {
                int groupMask = lastCut ^ mask;
                List<long[]> group = new ArrayList<>();
                for (int rest = groupMask; rest != 0; rest &= rest - 1) {
                    long[] member = members.get(Integer.numberOfTrailingZeros(rest));
                    group.add(new long[]{member[0], member[1]});
                }
                groups.add(group);
                lastCut = mask;
            }
        }
        return groups;
    }

    /**
     * Repeatedly matches the largest debtor with the largest creditor. Each payment zeroes at least
     * one of them and the final payment zeroes both, so a zero-sum group of size s needs at most s - 1.
     */
    private static void settleGreedily(List<long[]> members, List<Transfer> out) {
        Comparator<long[]> largestFirst = Comparator.<long[]>comparingLong(m -> Math.abs(m[1]))
                .reversed()
                .thenComparingLong(m -> m[0]);
        PriorityQueue<long[]> creditors = new PriorityQueue<>(largestFirst);
        PriorityQueue<long[]> debtors = new PriorityQueue<>(largestFirst);
        for (long[] member : members) {
            (member[1] > 0 ? creditors : debtors).add(new long[]{member[0], member[1]});
        }

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            long[] creditor = creditors.poll();
            long[] debtor = debtors.poll();
            long amount = Math.min(creditor[1], -debtor[1]);
            out.add(new Transfer(debtor[0], creditor[0], amount));

            creditor[1] -= amount;
            debtor[1] += amount;
            if (creditor[1] > 0) {
                creditors.add(creditor);
            }
            if (debtor[1] < 0) {
                debtors.add(debtor);
            }
        }
    }
}
