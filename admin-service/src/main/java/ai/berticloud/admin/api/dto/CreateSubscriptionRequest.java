package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateSubscriptionRequest(
    @NotBlank String tenantId,
    @NotBlank String status, // ACTIVE
    @NotNull Instant validFrom,
    @NotNull Instant validTo,
    @NotNull Integer maxDevices
) {}