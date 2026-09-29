package com.sharedexpenses.service;

import com.sharedexpenses.domain.Member;
import com.sharedexpenses.repository.ExpenseRepository;
import com.sharedexpenses.repository.MemberRepository;
import com.sharedexpenses.repository.SettlementRepository;
import com.sharedexpenses.settlement.DebtSimplifier;
import com.sharedexpenses.web.dto.Responses;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Balances are derived from the ledger (expenses + settlements) on every read rather than stored,
 * so they can never drift out of sync with the data. For each member:
 *
 * <pre>balance = paid for expenses - share of expenses + settlements sent - settlements received</pre>
 *
 * Every expense's shares sum to its amount and every settlement adds and subtracts the same value,
 * so the balances of a group always sum to exactly zero.
 */
@Service
@Transactional(readOnly = true)
public class BalanceService {

    private final GroupService groupService;
    private final MemberRepository members;
    private final ExpenseRepository expenses;
    private final SettlementRepository settlements;

    public BalanceService(GroupService groupService, MemberRepository members,
                          ExpenseRepository expenses, SettlementRepository settlements) {
        this.groupService = groupService;
        this.members = members;
        this.expenses = expenses;
        this.settlements = settlements;
    }

    public Responses.Balances getBalances(Long groupId) {
        groupService.requireGroup(groupId);
        List<Member> groupMembers = members.findByGroupIdOrderByIdAsc(groupId);
        Map<Long, Member> byId = groupMembers.stream().collect(Collectors.toMap(Member::getId, Function.identity()));

        Map<Long, Long> balances = computeBalanceCents(groupId, groupMembers);

        List<Responses.MemberBalance> memberBalances = groupMembers.stream()
                .map(m -> new Responses.MemberBalance(m.getId(), m.getName(), Money.fromCents(balances.get(m.getId()))))
                .toList();
        List<Responses.SuggestedPayment> suggestions = DebtSimplifier.simplify(balances).stream()
                .map(t -> new Responses.SuggestedPayment(
                        t.fromMemberId(), byId.get(t.fromMemberId()).getName(),
                        t.toMemberId(), byId.get(t.toMemberId()).getName(),
                        Money.fromCents(t.amountCents())))
                .toList();
        return new Responses.Balances(memberBalances, suggestions);
    }

    Map<Long, Long> computeBalanceCents(Long groupId, List<Member> groupMembers) {
        Map<Long, Long> balances = new LinkedHashMap<>();
        groupMembers.forEach(m -> balances.put(m.getId(), 0L));

        apply(balances, expenses.sumPaidByMember(groupId), 1);
        apply(balances, expenses.sumOwedByMember(groupId), -1);
        apply(balances, settlements.sumSentByMember(groupId), 1);
        apply(balances, settlements.sumReceivedByMember(groupId), -1);

        long total = balances.values().stream().mapToLong(Long::longValue).sum();
        if (total != 0) {
            throw new IllegalStateException("Ledger for group " + groupId + " is inconsistent: balances sum to " + total);
        }
        return balances;
    }

    private static void apply(Map<Long, Long> balances, List<Object[]> rows, int sign) {
        for (Object[] row : rows) {
            Long memberId = (Long) row[0];
            long cents = ((Number) row[1]).longValue();
            balances.merge(memberId, sign * cents, Long::sum);
        }
    }
}
