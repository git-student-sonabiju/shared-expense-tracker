package com.sharedexpenses.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A group of people sharing expenses. Named ExpenseGroup because GROUP is a reserved SQL word. */
@Entity
@Table(name = "expense_group")
public class ExpenseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    /** The account that created the group; only that account can see or change it. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Member> members = new ArrayList<>();

    protected ExpenseGroup() {
    }

    public ExpenseGroup(String name, UserAccount owner) {
        this.name = name;
        this.owner = owner;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Member addMember(String memberName) {
        Member member = new Member(this, memberName);
        members.add(member);
        return member;
    }

    public void removeMember(Member member) {
        members.remove(member);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Member> getMembers() {
        return members;
    }
}
