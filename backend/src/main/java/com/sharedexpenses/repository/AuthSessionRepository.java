package com.sharedexpenses.repository;

import com.sharedexpenses.domain.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    @Query("select s from AuthSession s join fetch s.user where s.tokenHash = :tokenHash")
    Optional<AuthSession> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from AuthSession s where s.tokenHash = :tokenHash")
    int deleteByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying
    @Query("delete from AuthSession s where s.expiresAt <= :now")
    int deleteExpired(@Param("now") Instant now);
}
