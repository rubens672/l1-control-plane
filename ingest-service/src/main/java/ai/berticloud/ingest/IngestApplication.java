package ai.berticloud.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Ingest Service (Data-plane) - Spring Boot entrypoint.
 *
 * RUOLO NEL SISTEMA:
 * - Backend Cloud Run che riceve telemetria device via HTTPS.
 * - Si fida SOLO dell'identità derivata dal certificato mTLS (SAN URI) validato dal Load Balancer.
 * - Pubblica la telemetria raw su Pub/Sub topic globale con attributi (tenantId/siteId/deviceId/...).
 *
 * NOTE:
 * - Questo servizio NON gestisce enrollment né CRUD: fa solo ingest veloce.
 * - In L1 usa cache Caffeine per ridurre query Cloud SQL (hot-path authz).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@SpringBootApplication
public class IngestApplication {
  public static void main(String[] args) {
    SpringApplication.run(IngestApplication.class, args);
  }
}