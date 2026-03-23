/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class DeviceDto {
    private String deviceId;
    private String tenantId;
    private String siteId;
    private String status;
    private String model;
    private Integer maxMsgsPerMin;
    private OffsetDateTime createdAt;
}
