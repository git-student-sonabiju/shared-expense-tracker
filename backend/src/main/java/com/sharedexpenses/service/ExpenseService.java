package com.sharedexpenses.service;

import com.sharedexpenses.domain.Expense;
import com.sharedexpenses.domain.ExpenseGroup;
import com.sharedexpenses.domain.Member;
import com.sharedexpenses.domain.SplitType;
import com.sharedexpenses.repository.ExpenseRepository;
import com.sharedexpenses.repository.MemberRepository;
import com.sharedexpenses.settlement.SplitCalculator;
import com.sharedexpenses.web.dto.Requests;
import com.sharedexpenses.web.dto.Responses;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExpenseService {

    private final GroupService groupService;
    private final ExpenseRepository expenses;
    private final MemberRepository members;

    public ExpenseService(GroupService groupService, ExpenseRepository expenses, MemberRepository members) {
        this.groupService = groupService;
        this.expenses = expenses;
        this.members = members;
    }

    @Transactional(readOnly = true)
    public List<Responses.ExpenseInfo> listExpenses(Long groupId) {
        groupService.requireGroup(groupId);
        return expenses.findAllWithSharesByGroupId(groupId).stream().map(Responses.ExpenseInfo::from).toList();
    }

    public Responses.ExpenseInfo addExpense(Long groupId, Requests.CreateExpense request) {
        ExpenseGroup group = groupService.requireGroup(groupId);
        Map<Long, Member> groupMembers = members.findByGroupIdOrderByIdAsc(groupId).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));

        Member payer = groupMembers.get(request.paidByMemberId());
        if (payer == null) {
            throw new InvalidRequestException("paidByMemberId", "must be a member of this group");
        }

        long totalCents = Money.toCents(request.amount(), "amount");
        Map<Long, Long> shares = computeShares(request, totalCents, groupMembers);

        Expense expense = new Expense(group, request.description().trim(), totalCents, payer, request.splitType());
        shares.forEach((memberId, cents) -> expense.addShare(groupMembers.get(memberId), cents));
        return Responses.ExpenseInfo.from(expenses.save(expense));
    }

    public void deleteExpense(Long groupId, Long expenseId) {
        groupService.requireGroup(groupId);
        Expense expense = expenses.findByIdAndGroupId(expenseId, groupId)
                .orElseThrow(() -> new NotFoundException("Expense " + expenseId + " not found in group " + groupId));
        expenses.delete(expense);
    }

    /** Validates participants and returns memberId -> share in cents; shares always sum to totalCents. */
    private Map<Long, Long> computeShares(Requests.CreateExpense request, long totalCents, Map<Long, Member> groupMembers) {
        List<Requests.Split> splits = request.splits();
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < splits.size(); i++) {
            Long memberId = splits.get(i).memberId();
            String field = "splits[" + i + "].memberId";
            if (!groupMembers.containsKey(memberId)) {
                throw new InvalidRequestException(field, "must be a member of this group");
            }
            if (!seen.add(memberId)) {
                throw new InvalidRequestException(field, "lists member " + memberId + " more than once");
            }
        }

        if (request.splitType() == SplitType.EQUAL) {
            for (int i = 0; i < splits.size(); i++) {
                if (splits.get(i).amount() != null) {
                    throw new InvalidRequestException("splits[" + i + "].amount", "must be omitted for an EQUAL split");
                }
            }
            if (totalCents < splits.size()) {
                throw new InvalidRequestException("amount", "is too small to split among " + splits.size() + " members");
            }
            List<Long> ordered = splits.stream().map(Requests.Split::memberId).sorted().toList();
            return SplitCalculator.splitEqually(totalCents, ordered);
        }

        Map<Long, Long> shares = new LinkedHashMap<>();
        long sum = 0;
        for (int i = 0; i < splits.size(); i++) {
            Requests.Split split = splits.get(i);
            long cents = Money.toCents(split.amount(), "splits[" + i + "].amount");
            shares.put(split.memberId(), cents);
            sum += cents;
        }
        if (sum != totalCents) {
            throw new InvalidRequestException("splits", "must add up to the expense amount "
                    + Money.fromCents(totalCents) + " but add up to " + Money.fromCents(sum));
        }
        return shares;
    }
}
