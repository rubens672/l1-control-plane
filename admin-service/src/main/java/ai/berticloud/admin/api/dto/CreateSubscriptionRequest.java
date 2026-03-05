package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Request per creare/aggiornare la subscription (contratto) di un tenant.
 *
 * Endpoint:
 * - POST /v1/admin/subscriptions
 *
 * Campi importanti per ingest-service:
 * - status: deve essere ACTIVE per consentire telemetria
 * - validTo: se scaduta -> ingest rifiuta (403 sub_expired)
 * - maxDevices: limite contrattuale (in L1 non ancora enforce in runtime)
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record CreateSubscriptionRequest(
    @NotBlank String tenantId,  // FK verso tenants
    @NotBlank String status,    // es: ACTIVE / PAST_DUE / CANCELED / EXPIRED
    @NotNull Instant validFrom,
    @NotNull Instant validTo,
    @NotNull Integer maxDevices
) {}