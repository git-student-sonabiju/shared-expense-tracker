package com.sharedexpenses.domain;

import jakarta.persistence.*;

/** The portion of an expense owed by a single member. */
@Entity
@Table(name = "expense_share",
        uniqueConstraints = @UniqueConstraint(name = "uk_share_expense_member", columnNames = {"expense_id", "member_id"}))
public class ExpenseShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private long amountCents;

    protected ExpenseShare() {
    }

    ExpenseShare(Expense expense, Member member, long amountCents) {
        this.expense = expense;
        this.member = member;
        this.amountCents = amountCents;
    }

    public Long getId() {
        return id;
    }

    public Expense getExpense() {
        return expense;
    }

    public Member getMember() {
        return member;
    }

    public long getAmountCents() {
        return amountCents;
    }
}
