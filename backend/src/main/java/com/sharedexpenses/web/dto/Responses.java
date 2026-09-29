package com.sharedexpenses.web.dto;

import com.sharedexpenses.domain.*;
import com.sharedexpenses.service.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Response bodies returned by the API. Amounts are decimals with exactly 2 places. */
public final class Responses {

    private Responses() {
    }

    public record GroupSummary(Long id, String name, long memberCount, Instant createdAt) {
    }

    public record Group(Long id, String name, Instant createdAt, List<MemberInfo> members) {
        public static Group from(ExpenseGroup group) {
            return new Group(group.getId(), group.getName(), group.getCreatedAt(),
                    group.getMembers().stream().map(MemberInfo::from).toList());
        }
    }

    public record MemberInfo(Long id, String name) {
        public static MemberInfo from(Member member) {
            return new MemberInfo(member.getId(), member.getName());
        }
    }

    public record Share(Long memberId, String memberName, BigDecimal amount) {
    }

    public record ExpenseInfo(Long id, String description, BigDecimal amount, MemberInfo paidBy,
                              SplitType splitType, Instant createdAt, List<Share> shares) {
        public static ExpenseInfo from(Expense expense) {
            List<Share> shares = expense.getShares().stream()
                    .map(s -> new Share(s.getMember().getId(), s.getMember().getName(), Money.fromCents(s.getAmountCents())))
                    .toList();
            return new ExpenseInfo(expense.getId(), expense.getDescription(), Money.fromCents(expense.getAmountCents()),
                    MemberInfo.from(expense.getPaidBy()), expense.getSplitType(), expense.getCreatedAt(), shares);
        }
    }

    public record SettlementInfo(Long id, MemberInfo from, MemberInfo to, BigDecimal amount, String note, Instant createdAt) {
        public static SettlementInfo from(Settlement settlement) {
            return new SettlementInfo(settlement.getId(), MemberInfo.from(settlement.getFromMember()),
                    MemberInfo.from(settlement.getToMember()), Money.fromCents(settlement.getAmountCents()),
                    settlement.getNote(), settlement.getCreatedAt());
        }
    }

    /** {@code balance} is positive when the member is owed money and negative when they owe. */
    public record MemberBalance(Long memberId, String memberName, BigDecimal balance) {
    }

    public record SuggestedPayment(Long fromMemberId, String fromMemberName,
                                   Long toMemberId, String toMemberName, BigDecimal amount) {
    }

    public record Balances(List<MemberBalance> balances, List<SuggestedPayment> suggestedPayments) {
    }
}
