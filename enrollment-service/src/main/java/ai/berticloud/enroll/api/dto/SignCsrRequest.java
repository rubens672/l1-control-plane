package ai.berticloud.enroll.api.dto;

import jakarta.validation.constraints.NotBlank;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Request body per l'endpoint di enrollment CSR signing.
 *
 * Endpoint:
 * - POST /v1/enrollments/{deviceId}:signCsr
 *
 * Sicurezza / Trust:
 * - La CSR viene dal device ed è un input NON trusted.
 * - La CSR deve contenere una SAN URI conforme alla convention:
 *     urn:berticloudai:tenant:<TENANT>:site:<SITE>:device:<DEVICE>
 * - La validazione della SAN avviene server-side:
 *   - match con deviceId nel path
 *   - match con tenant/site registrati nel DB
 *
 * Campo:
 * - csrPem: CSR in formato PEM (PKCS#10), inclusi header/footer.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record SignCsrRequest(
    @NotBlank String csrPem // PEM PKCS#10 CSR: -----BEGIN CERTIFICATE REQUEST----- ...
) {}