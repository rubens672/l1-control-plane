/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;

@Data
public class BootstrapTokenResponse {
    private String tenantId;
    private String siteId;
    private String deviceId;
    private String bootstrapToken;
    private String enrollmentUrl;
    private String telemetryUrl;
}
