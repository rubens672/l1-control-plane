package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Request per creare un site (installazione/impianto) di un tenant.
 *
 * Endpoint:
 * - POST /v1/admin/sites
 *
 * Note:
 * - siteId entra nella SAN URN.
 * - timezone serve per reporting/dashboard lato cliente (L1).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record CreateSiteRequest(
    @NotBlank String siteId,    // es: site-roma-001
    @NotBlank String tenantId,  // es: tnt-001
    @NotBlank String name,      // es: "Villa Roma"
    String timezone,            // es: "Europe/Rome"
    String status               // ACTIVE
) {}