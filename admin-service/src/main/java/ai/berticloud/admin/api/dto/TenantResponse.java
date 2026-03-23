/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.admin.api.dto;

import java.time.Instant;

public record TenantResponse(
    String tenantId,
    String name,
    String status,
    String plan,
    Instant createdAt,
    Instant updatedAt
) {}
