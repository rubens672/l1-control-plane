package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Request per creare un tenant (cliente).
 *
 * Endpoint:
 * - POST /v1/admin/tenants
 *
 * Note:
 * - tenantId è l'identificativo stabile e compare anche nella SAN URN dei certificati.
 * - In L1 tenant viene creato con status ACTIVE di default lato DB.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record CreateTenantRequest(
    @NotBlank String tenantId, // es: tnt-001
    @NotBlank String name,     // es: "Cliente Roma"
    String plan                // es: "BASE", "PRO", "PLUS" (informativo in L1)
) {}