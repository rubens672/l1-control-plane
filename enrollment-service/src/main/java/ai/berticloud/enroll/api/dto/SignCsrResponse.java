package ai.berticloud.enroll.api.dto;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Response dell'enrollment CSR signing.
 *
 * Contiene:
 * - clientCertPem: certificato client firmato (PEM) da installare sul device
 * - chainPem: catena CA (PEM) da installare sul device (per trust chain)
 *
 * Note operative:
 * - Il device deve conservare la private key generata localmente (client.key)
 * - clientCertPem + chainPem verranno usati per mTLS verso ingest-service
 *
 * Nota:
 * - In L1 la chain può coincidere con issuingCert; in L2 può includere intermediate+root.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public record SignCsrResponse(
    String clientCertPem,
    String chainPem
) {}