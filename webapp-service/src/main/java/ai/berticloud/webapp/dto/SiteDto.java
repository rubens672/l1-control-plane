/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class SiteDto {
    private String siteId;
    private String tenantId;
    private String name;
    private String timezone;
    private String status;
    private OffsetDateTime createdAt;
}
