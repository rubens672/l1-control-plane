package ai.berticloud.enroll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Enrollment Service (Control-plane / Security) - Spring Boot entrypoint.
 *
 * RUOLO NEL SISTEMA:
 * - Gestisce l'enrollment CSR-based dei device.
 * - Firma certificati client X.509 usando una issuing CA custodita lato server.
 *
 * PERCHÉ ESISTE:
 * - I device NON devono avere chiavi GCP.
 * - La private key del device deve rimanere sul device (mai trasferita).
 * - L'identità forte del device viene codificata nel certificato (SAN URI).
 *
 * FLUSSO (L1):
 * 1) Admin crea device in DB come PENDING e genera bootstrap token (1 ora).
 * 2) Device genera keypair locale + CSR con SAN URN:
 *      urn:berticloudai:tenant:<TENANT>:site:<SITE>:device:<DEVICE>
 * 3) Device chiama:
 *      POST /v1/enrollments/{deviceId}:signCsr
 *    con Authorization: Bearer <bootstrapToken> e CSR PEM.
 * 4) Enrollment-service:
 *    - verifica bootstrap token (hash HMAC vs DB) e scadenza
 *    - valida SAN del CSR (match deviceId path + tenant/site in DB)
 *    - firma un certificato client (EKU clientAuth) copiando la SAN
 *    - calcola fingerprint SHA256 (DER)
 *    - aggiorna DB in transazione: device -> ACTIVE + fingerprint + history
 *
 * OUTPUT:
 * - Certificato client PEM + chain PEM da installare sul device.
 *
 * DIPENDENZE ESTERNE:
 * - Cloud SQL Postgres (control-plane DB)
 * - Secret Manager (issuingCA private key + cert + chain)
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@SpringBootApplication
public class EnrollmentApplication {
  public static void main(String[] args) {
    SpringApplication.run(EnrollmentApplication.class, args);
  }
}