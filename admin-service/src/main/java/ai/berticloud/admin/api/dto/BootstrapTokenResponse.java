package ai.berticloud.admin.api.dto;

import java.time.Instant;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Response dell'emissione bootstrap token.
 *
 * Endpoint:
 * - POST /v1/admin/devices/{deviceId}:bootstrapToken
 *
 * ATTENZIONE (security):
 * - bootstrapToken viene mostrato SOLO in questa response.
 * - NON è recuperabile dal DB (il DB salva solo hash HMAC).
 * - Deve essere consegnato al cliente/device in modo sicuro (es. copia manuale).
 *
 * Uso:
 * - Il device userà questo token come:
 *     Authorization: Bearer <bootstrapToken>
 *   per chiamare enrollment-service /signCsr.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record BootstrapTokenResponse(
        String tenantId,
        String siteId,
        String deviceId,
        String bootstrapToken,  // segreto temporaneo one-time
        String enrollmentUrl,
        String telemetryUrl,
        Boolean telemetryUseTls,
        Boolean localTestHeaders
) {}