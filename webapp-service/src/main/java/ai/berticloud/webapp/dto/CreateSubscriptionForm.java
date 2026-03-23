/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;

@Data
public class CreateSubscriptionForm {
    private String tenantId;
    private Integer validDays = 365;
    private Integer maxDevices = 10;
}
