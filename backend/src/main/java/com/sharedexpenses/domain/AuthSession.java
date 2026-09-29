package com.sharedexpenses.domain;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A logged-in session. The client holds a random bearer token; only its SHA-256 hash is stored,
 * so a leaked database does not expose usable tokens. Logging out deletes the row.
 */
@Entity
@Table(name = "auth_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_session_token_hash", columnNames = "token_hash"))
public class AuthSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    protected AuthSession() {
    }

    public AuthSession(String tokenHash, UserAccount user, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public UserAccount getUser() {
        return user;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
