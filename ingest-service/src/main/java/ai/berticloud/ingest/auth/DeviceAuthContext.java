package ai.berticloud.ingest.auth;

import java.time.Instant;

public record DeviceAuthContext(
    String tenantId,
    String siteId,
    String deviceId,
    String tenantStatus,
    String siteStatus,
    String deviceStatus,
    String subscriptionStatus,
    Instant subscriptionValidTo,
    String expectedFingerprintSha256,
    int maxMsgsPerMin
) {}