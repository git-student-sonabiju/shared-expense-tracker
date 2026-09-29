package com.sharedexpenses.web.dto;

import com.sharedexpenses.domain.SplitType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/** Request bodies accepted by the API. Structural validation lives here; business rules in the services. */
public final class Requests {

    private Requests() {
    }

    public record CreateGroup(
            @NotBlank(message = "is required") @Size(max = 100, message = "must be at most 100 characters")
            String name,
            @Size(max = 50, message = "must contain at most 50 members")
            List<@NotBlank(message = "must not be blank") @Size(max = 60, message = "must be at most 60 characters") String> members) {
    }

    public record AddMember(
            @NotBlank(message = "is required") @Size(max = 60, message = "must be at most 60 characters")
            String name) {
    }

    public record RenameMember(
            @NotBlank(message = "is required") @Size(max = 60, message = "must be at most 60 characters")
            String name) {
    }

    public record CreateExpense(
            @NotBlank(message = "is required") @Size(max = 200, message = "must be at most 200 characters")
            String description,
            @NotNull(message = "is required")
            @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 10, fraction = 2, message = "must be a number with at most 2 decimal places")
            BigDecimal amount,
            @NotNull(message = "is required")
            Long paidByMemberId,
            @NotNull(message = "is required (EQUAL or EXACT)")
            SplitType splitType,
            @NotEmpty(message = "must include at least one member")
            List<@NotNull(message = "must not be null") @Valid Split> splits) {
    }

    /** One participant of an expense. {@code amount} is required for EXACT splits and must be omitted for EQUAL. */
    public record Split(
            @NotNull(message = "is required")
            Long memberId,
            @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 10, fraction = 2, message = "must be a number with at most 2 decimal places")
            BigDecimal amount) {
    }

    public record CreateSettlement(
            @NotNull(message = "is required")
            Long fromMemberId,
            @NotNull(message = "is required")
            Long toMemberId,
            @NotNull(message = "is required")
            @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 10, fraction = 2, message = "must be a number with at most 2 decimal places")
            BigDecimal amount,
            @Size(max = 200, message = "must be at most 200 characters")
            String note) {
    }
}
