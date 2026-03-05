package ai.berticloud.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Admin Service (Control-plane) - Spring Boot entrypoint.
 *
 * RUOLO NEL SISTEMA:
 * - Backend "control-plane" che gestisce anagrafiche e governance:
 *   tenant, subscription, sites, devices.
 * - È usato dall'admin (te / webapp futura) e NON dai device (tranne indirettamente via bootstrap token).
 *
 * COSA FA IN L1:
 * - CRUD minimo per creare tenant/site/subscription/device (device nasce PENDING).
 * - Emissione bootstrap token one-time con TTL (es. 60 min) per enrollment CSR.
 *
 * SICUREZZA (L1):
 * - Il bootstrap token è un segreto temporaneo.
 * - Viene salvato nel DB SOLO come hash HMAC (mai in chiaro).
 * - Il token plaintext viene mostrato SOLO nella risposta della chiamata di emissione.
 *
 * DIPENDENZE ESTERNE:
 * - Cloud SQL (Postgres) come source of truth del control-plane.
 * - (Opzionale) Secret Manager per custodire la chiave HMAC (in L1 può essere env var).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@SpringBootApplication
public class AdminApplication {
  public static void main(String[] args) {
    SpringApplication.run(AdminApplication.class, args);
  }
}