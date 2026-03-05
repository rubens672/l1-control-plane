package ai.berticloud.admin.api.dto;

import java.time.Instant;

public record BootstrapTokenResponse(
    String deviceId,
    String bootstrapToken,
    Instant expiresAt
) {}