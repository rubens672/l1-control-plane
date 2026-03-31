package ai.berticloud.enroll.ca.issuer;

import java.time.Instant;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Record DTO che rappresenta il certificato firmato restituito dal Certificate Issuer.
 * Include la catena del certificato in formato PEM e i metadati da registrare nel DB.
 * 
 * @author Antonio Berti
 * @version 1.0
 * @since 31 March 2026
 */
public record IssuedCertificate(
    String clientCertPem,
    String chainPem,
    String serialNumber,
    Instant notAfter
) {}
