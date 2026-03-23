/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SubscriptionDto {
    private String tenantId;
    private String status;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Integer maxDevices;
    private OffsetDateTime updatedAt;
}
