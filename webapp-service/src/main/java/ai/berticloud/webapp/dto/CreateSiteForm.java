/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.dto;

import lombok.Data;

@Data
public class CreateSiteForm {
    private String tenantId;
    private String name;
    private String timezone = "Europe/Rome";
}
