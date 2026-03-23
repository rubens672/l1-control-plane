/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.admin.api.dto;

import java.time.Instant;

public record DeviceResponse(
    String deviceId,
    String tenantId,
    String siteId,
    String status,
    String model,
    Integer maxMsgsPerMin,
    Instant createdAt
) {}
