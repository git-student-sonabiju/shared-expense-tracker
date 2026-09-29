package com.sharedexpenses.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An expense paid by one member and split among one or more members.
 * Invariant (enforced by ExpenseService): the shares add up to exactly amountCents.
 */
@Entity
@Table(name = "expense", indexes = @Index(name = "idx_expense_group", columnList = "group_id"))
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false)
    private long amountCents;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by_id", nullable = false)
    private Member paidBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SplitType splitType;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ExpenseShare> shares = new ArrayList<>();

    protected Expense() {
    }

    public Expense(ExpenseGroup group, String description, long amountCents, Member paidBy, SplitType splitType) {
        this.group = group;
        this.description = description;
        this.amountCents = amountCents;
        this.paidBy = paidBy;
        this.splitType = splitType;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void addShare(Member member, long shareCents) {
        shares.add(new ExpenseShare(this, member, shareCents));
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public String getDescription() {
        return description;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public Member getPaidBy() {
        return paidBy;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ExpenseShare> getShares() {
        return shares;
    }
}
