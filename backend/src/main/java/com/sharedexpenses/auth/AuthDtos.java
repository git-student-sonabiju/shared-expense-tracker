package com.sharedexpenses.auth;

import com.sharedexpenses.domain.UserAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record Register(
            @NotBlank(message = "is required") @Size(max = 30, message = "is too long")
            String username,
            @NotBlank(message = "is required") @Size(max = 60, message = "must be at most 60 characters")
            String displayName,
            @NotBlank(message = "is required")
            @Size(min = 8, max = 100, message = "must be at least 8 characters")
            String password) {
    }

    public record Login(
            @NotBlank(message = "is required") String username,
            @NotBlank(message = "is required") String password) {
    }

    public record UserInfo(Long id, String username, String displayName) {
        public static UserInfo from(UserAccount user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getDisplayName());
        }
    }

    public record Session(String token, Instant expiresAt, UserInfo user) {
    }
}
