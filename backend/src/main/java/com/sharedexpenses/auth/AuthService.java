package com.sharedexpenses.auth;

import com.sharedexpenses.domain.AuthSession;
import com.sharedexpenses.domain.UserAccount;
import com.sharedexpenses.repository.AuthSessionRepository;
import com.sharedexpenses.repository.GroupRepository;
import com.sharedexpenses.repository.UserRepository;
import com.sharedexpenses.service.ConflictException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
@Transactional
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();
    /** Compared against when the username does not exist, so both failure paths take the same time. */
    private static final String DUMMY_HASH = ENCODER.encode("not-a-real-password");

    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final GroupRepository groups;
    private final Duration sessionTtl;

    public AuthService(UserRepository users, AuthSessionRepository sessions, GroupRepository groups,
                       @Value("${app.auth.session-ttl:7d}") Duration sessionTtl) {
        this.users = users;
        this.sessions = sessions;
        this.groups = groups;
        this.sessionTtl = sessionTtl;
    }

    public AuthDtos.Session register(AuthDtos.Register request) {
        String username = normalize(request.username());
        if (users.existsByUsername(username)) {
            throw new ConflictException("The username '" + username + "' is already taken");
        }
        boolean firstUser = users.count() == 0;
        UserAccount user = users.saveAndFlush(new UserAccount(username, request.displayName().trim(),
                ENCODER.encode(request.password())));
        if (firstUser) {
            // Groups created before login existed have no owner; hand them to the first account.
            groups.assignOrphanGroupsTo(user);
        }
        return startSession(user);
    }

    public AuthDtos.Session login(AuthDtos.Login request) {
        UserAccount user = users.findByUsername(normalize(request.username())).orElse(null);
        boolean matches = ENCODER.matches(request.password(), user == null ? DUMMY_HASH : user.getPasswordHash());
        if (user == null || !matches) {
            throw new UnauthorizedException("Invalid username or password");
        }
        sessions.deleteExpired(Instant.now());
        return startSession(user);
    }

    public void logout(String token) {
        sessions.deleteByTokenHash(hash(token));
    }

    /** Resolves a bearer token to its user id, rejecting unknown or expired tokens. */
    @Transactional(readOnly = true)
    public Long authenticate(String token) {
        AuthSession session = sessions.findByTokenHash(hash(token))
                .orElseThrow(() -> new UnauthorizedException("Your session is invalid. Please log in again"));
        if (session.isExpired(Instant.now())) {
            throw new UnauthorizedException("Your session has expired. Please log in again");
        }
        return session.getUser().getId();
    }

    @Transactional(readOnly = true)
    public AuthDtos.UserInfo me(Long userId) {
        return users.findById(userId).map(AuthDtos.UserInfo::from)
                .orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
    }

    private AuthDtos.Session startSession(UserAccount user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant expiresAt = Instant.now().plus(sessionTtl);
        sessions.save(new AuthSession(hash(token), user, expiresAt));
        return new AuthDtos.Session(token, expiresAt, AuthDtos.UserInfo.from(user));
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
