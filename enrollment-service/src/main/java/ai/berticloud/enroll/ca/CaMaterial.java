package ai.berticloud.enroll.ca;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Contenitore del materiale crittografico della issuing CA.
 *
 * Contiene:
 * - issuingKey: private key della CA (server-side, segreta)
 * - issuingCert: certificato pubblico della CA
 * - chainPem: catena CA da consegnare al device (per validare server-side e/o client-side)
 *
 * NOTE:
 * - issuingKey deve restare server-side (Secret Manager).
 * - chainPem può includere issuingCert + eventuali intermediate.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 202
 */
public record CaMaterial(
    PrivateKey issuingKey,
    X509Certificate issuingCert,
    String chainPem
) {}