/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;

@Data
public class CreateDeviceForm {
    private String tenantId;
    private String siteId;
    private String model;
    private Integer maxMsgsPerMin = 60;
}
