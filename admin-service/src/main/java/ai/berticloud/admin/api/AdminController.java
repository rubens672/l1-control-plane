package ai.berticloud.admin.api;

import ai.berticloud.admin.api.dto.*;
import ai.berticloud.admin.db.AdminRepository;
import ai.berticloud.admin.security.BootstrapTokenIssuer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * REST Controller del control-plane (admin-service).
 *
 * ENDPOINT PRINCIPALI (L1):
 * - POST /v1/admin/tenants
 * - POST /v1/admin/subscriptions
 * - POST /v1/admin/sites
 * - POST /v1/admin/devices
 * - POST /v1/admin/devices/{deviceId}:bootstrapToken
 *
 * FLUSSO LOGICO:
 * 1) l'admin crea tenant/subscription/site/device (device = PENDING)
 * 2) l'admin genera bootstrap token per il device (one-time, TTL 1 ora)
 * 3) il cliente usa quel token per chiamare enrollment-service e ottenere il cert mTLS
 *
 * TRUST & SECURITY:
 * - Queste API sono "privilegiate": devono essere protette (IAM / auth webapp / IP allowlist / etc).
 * - Il token bootstrap NON viene mai salvato in chiaro:
 *   - DB salva solo bootstrap_token_hash (HMAC)
 *   - response contiene token plaintext per consegna al cliente
 *
 * NOTE OPERATIVE:
 * - Per semplicità L1 usiamo "insert/upsert" con JdbcTemplate in repository.
 * - La gestione di ruoli admin/console verrà raffinata quando arriva la webapp.
 *
 *
 * File di provisioning
 *
 * L’admin genera un piccolo file json, ad esempio:
 *
 * {
 *   "deviceId": "dev-001",
 *   "tenantId": "tnt-001",
 *   "enrollmentUrl": "https://enroll.example.com",
 *   "bootstrapToken": "CqSBU6Zr0P8BlEvqVTI1Kd6n0YO90zQZXdX7cbakevo",
 *   "expiresAt": "2026-03-10T18:00:00Z"
 * }
 *
 * questo file è copiato su USB e importato nel device al primo avvio
 *
 *
 * Come fa il device a usare il bootstrapToken?
 *
 * Il device deve avere un piccolo bootstrap agent già preinstallato.
 * Quel componente fa questo:
 * legge deviceId e bootstrapToken
 * chiama enrollment-service via HTTPS
 * invia token + device metadata minimi
 * se token valido e device PENDING, il server:
 * marca il token come usato
 * emette cert + key oppure CSR flow
 * porta il device a stato ACTIVE o ENROLLED
 * il device salva il materiale mTLS in una directory sicura
 * da quel momento comunica solo con certificato, non più con token
 *
 * //urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@RestController
@RequestMapping("/v1/admin")
public class AdminController {
  private static final Logger log = LoggerFactory.getLogger(AdminController.class);

  private final AdminRepository repo;
  private final BootstrapTokenIssuer issuer;

  public AdminController(AdminRepository repo, BootstrapTokenIssuer issuer) {
    this.repo = repo;
    this.issuer = issuer;
  }

  /**
   * Crea un tenant (anagrafica cliente).
   * In L1 setta status = ACTIVE di default.
   */
  @PostMapping("/tenants")
  public ResponseEntity<?> createTenant(@Valid @RequestBody CreateTenantRequest r) {
    log.info("Received request to create tenant: {}", r.tenantId());
    try {
      repo.createTenant(r.tenantId(), r.name(), r.plan());
      log.info("Successfully created tenant: {}", r.tenantId());
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Failed to create tenant: {}", r.tenantId(), e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to create tenant"));
    }
  }

  /**
   * Crea/aggiorna la subscription del tenant (contratto/abbonamento).
   * È l'oggetto principale che governa validità e limiti (max devices, features).
   */
  @PostMapping("/subscriptions")
  public ResponseEntity<?> upsertSubscription(@Valid @RequestBody CreateSubscriptionRequest r) {
    log.info("Received request to upsert subscription for tenant: {}", r.tenantId());
    try {
      repo.upsertSubscription(r.tenantId(), r.status(), r.validFrom(), r.validTo(), r.maxDevices());
      log.info("Successfully upserted subscription for tenant: {}", r.tenantId());
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Failed to upsert subscription for tenant: {}", r.tenantId(), e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to upsert subscription"));
    }
  }

  /**
   * Crea un site (installazione fisica/cliente).
   * Un tenant può avere più site.
   */
  @PostMapping("/sites")
  public ResponseEntity<?> createSite(@Valid @RequestBody CreateSiteRequest r) {
    log.info("Received request to create site: {} for tenant: {}", r.siteId(), r.tenantId());
    try {
      repo.createSite(r.siteId(), r.tenantId(), r.name(), r.timezone(), r.status());
      log.info("Successfully created site: {}", r.siteId());
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Failed to create site: {}", r.siteId(), e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to create site"));
    }
  }

  /**
   * Registra un device nel control-plane.
   *
   * NOTE:
   * - Il device nasce in stato PENDING perché non ha ancora un certificato mTLS firmato.
   * - L'enrollment-service, dopo la firma del CSR, porterà il device ad ACTIVE.
   */
  @PostMapping("/devices")
  public ResponseEntity<?> createDevice(@Valid @RequestBody CreateDeviceRequest r) {
    log.info("Received request to register pending device: {} for tenant: {}", r.deviceId(), r.tenantId());
    try {
      repo.createDevicePending(r.deviceId(), r.tenantId(), r.siteId(), r.model());
      log.info("Successfully registered pending device: {}", r.deviceId());
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Failed to register device: {}", r.deviceId(), e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to register device"));
    }
  }

  /**
   * Elimina il device.
   * @param deviceId Identificativo device.
   * @return Numero di record eliminati.
   */
  @PostMapping("/devices/{deviceId}:delete")
  public ResponseEntity<?> deleteDevice(@PathVariable("deviceId") String deviceId) {
    log.info("Received request to delete device: {}", deviceId);
    try {
      int rows = repo.deleteDeviceById(deviceId);
      if (rows == 0) {
        log.warn("Device not found for deletion: {}", deviceId);
        return ResponseEntity.status(404).body(Map.of("error", "device_not_found"));
      }
      log.info("Successfully deleted device: {}", deviceId);
      return ResponseEntity.ok().build();
    } catch (Exception e) {
      log.error("Failed to delete device: {}", deviceId, e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to delete device"));
    }
  }

  /**
   * Emette un bootstrap token one-time per un device PENDING.
   *
   * OUTPUT:
   * - Ritorna token plaintext (da consegnare al cliente).
   * - Ritorna expiresAt.
   *
   * DB:
   * - Salva SOLO l'hash HMAC del token + expiry sul device.
   *
   * SICUREZZA:
   * - Se qualcuno ruba il DB, non ottiene il token plaintext (solo hash).
   * - Il token è temporaneo: TTL ~60 minuti (config).
   */
  @PostMapping("/devices/{deviceId}:bootstrapToken")
  public ResponseEntity<?> issueBootstrap(@PathVariable("deviceId") String deviceId) {
    log.info("Received request to issue bootstrap token for device: {}", deviceId);
    try {
      var t = issuer.issueOneTimeToken(deviceId);
      repo.setBootstrapToken(deviceId, t.tokenHashHex(), t.expiresAt());
      log.info("Successfully issued bootstrap token for device: {}", deviceId);
      return ResponseEntity.ok(new BootstrapTokenResponse(deviceId, t.tokenPlain(), t.enrollmentUrl(), t.expiresAt()));
    } catch (Exception e) {
      log.error("Failed to issue bootstrap token for device: {}", deviceId, e);
      return ResponseEntity.status(500).body(Map.of("error", "Failed to issue bootstrap token"));
    }
  }
}