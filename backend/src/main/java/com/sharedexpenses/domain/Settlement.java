package com.sharedexpenses.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** A payment of money from one member to another, reducing what the payer owes. */
@Entity
@Table(name = "settlement", indexes = @Index(name = "idx_settlement_group", columnList = "group_id"))
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_member_id", nullable = false)
    private Member fromMember;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_member_id", nullable = false)
    private Member toMember;

    @Column(nullable = false)
    private long amountCents;

    @Column(length = 200)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Settlement() {
    }

    public Settlement(ExpenseGroup group, Member fromMember, Member toMember, long amountCents, String note) {
        this.group = group;
        this.fromMember = fromMember;
        this.toMember = toMember;
        this.amountCents = amountCents;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public Member getFromMember() {
        return fromMember;
    }

    public Member getToMember() {
        return toMember;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getNote() {
        return note;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
