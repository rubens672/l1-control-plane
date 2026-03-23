/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.admin.api.dto;

import java.time.Instant;

public record SiteResponse(
    String siteId,
    String tenantId,
    String name,
    String timezone,
    String status,
    Instant createdAt
) {}
