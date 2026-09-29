package com.sharedexpenses.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "member",
        uniqueConstraints = @UniqueConstraint(name = "uk_member_group_name", columnNames = {"group_id", "name"}))
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private ExpenseGroup group;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Member() {
    }

    Member(ExpenseGroup group, String name) {
        this.group = group;
        this.name = name;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public Long getId() {
        return id;
    }

    public ExpenseGroup getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
