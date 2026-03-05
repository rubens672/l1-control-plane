package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Request per registrare un device nel control-plane.
 *
 * Endpoint:
 * - POST /v1/admin/devices
 *
 * Output DB:
 * - device viene creato in stato PENDING (non può ancora inviare telemetria)
 * - l'enrollment-service lo porterà ad ACTIVE dopo firma CSR e salvataggio fingerprint
 *
 * Note:
 * - deviceId entra nella SAN URN ed è quindi parte dell'identità crittografica.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record CreateDeviceRequest(
    @NotBlank String deviceId,  // es: rpi-123
    @NotBlank String tenantId,  // es: tnt-001
    @NotBlank String siteId,    // es: site-roma-001
    String model                // es: raspberry-pi4 (informativo)
) {}