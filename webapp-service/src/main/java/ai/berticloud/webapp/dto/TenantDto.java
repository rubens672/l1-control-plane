/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class TenantDto {
    private String tenantId;
    private String name;
    private String status;
    private String plan;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
