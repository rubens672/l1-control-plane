package ai.berticloud.admin.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import ai.berticloud.admin.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Repository JDBC per il control-plane DB (Cloud SQL Postgres).
 *
 * RUOLO:
 * - Implementa le operazioni DB necessarie a L1:
 *   - create tenant
 *   - upsert subscription
 *   - create site
 *   - create device (PENDING)
 *   - set bootstrap token hash + expiry sul device
 *
 * PERCHÉ JdbcTemplate:
 * - L1 vuole "ruote su strada": semplicità, trasparenza, zero magia.
 * - Qui stiamo principalmente facendo insert/update semplici e una upsert.
 *
 * NOTE DI INTEGRITÀ:
 * - La validazione di FK/PK è demandata a Postgres (vincoli).
 * - Lo status PENDING→ACTIVE è gestito dall'enrollment-service.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@Repository
public class AdminRepository {
  private static final Logger log = LoggerFactory.getLogger(AdminRepository.class);

  private final JdbcTemplate jdbc;

  public AdminRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Crea tenant con status ACTIVE di default (L1). */
  public void createTenant(String tenantId, String name, String plan) {
    jdbc.update("""
      INSERT INTO control_plane.tenants(tenant_id, name, status, plan, created_at, updated_at)
      VALUES(?, ?, 'ACTIVE', ?, now(), now())
      """, tenantId, name, plan);
  }

  /**
   * Crea o aggiorna subscription del tenant.
   *
   * NOTE:
   * - ON CONFLICT(tenant_id): un tenant ha una subscription "corrente" in L1.
   * - In L2 potresti avere history contratti o più piani.
   */
  public void upsertSubscription(String tenantId, String status, Instant validFrom, Instant validTo, int maxDevices) {
    jdbc.update("""
      INSERT INTO control_plane.subscriptions(tenant_id, status, valid_from, valid_to, max_devices, features, updated_at)
      VALUES(?, ?, ?, ?, ?, '{}'::jsonb, now())
      ON CONFLICT (tenant_id)
      DO UPDATE SET status = EXCLUDED.status, valid_from = EXCLUDED.valid_from, valid_to = EXCLUDED.valid_to,
                    max_devices = EXCLUDED.max_devices, updated_at = now()
      """, tenantId, status, Timestamp.from(validFrom), Timestamp.from(validTo), maxDevices);
  }

  /** Crea site collegato a tenant. */
  public void createSite(String siteId, String tenantId, String name, String timezone, String status) {
    jdbc.update("""
      INSERT INTO control_plane.sites(site_id, tenant_id, name, timezone, status, created_at)
      VALUES(?, ?, ?, ?, ?, now())
      """, siteId, tenantId, name, timezone, status == null ? "ACTIVE" : status);
  }

  /**
   * Registra un device nel control-plane in stato PENDING.
   *
   * PERCHÉ PENDING:
   * - Il device non ha ancora un certificato client firmato.
   * - Finché non completa enrollment CSR, non può inviare telemetria (ingest rifiuta).
   */
  public void createDevicePending(String deviceId, String tenantId, String siteId, String model) {
    jdbc.update("""
      INSERT INTO control_plane.devices(
        device_id, tenant_id, site_id, status, model, onboarded_at, last_seen_at,
        max_msgs_per_min, expected_fingerprint_sha256, cert_serial, cert_not_after,
        issuer_dn, subject_dn, bootstrap_token_hash, bootstrap_expires_at
      )
      VALUES(?, ?, ?, 'PENDING', ?, NULL, NULL, 60, NULL, NULL, NULL, NULL, NULL, NULL, NULL)
      """, deviceId, tenantId, siteId, model);
  }

  public int deleteDeviceById(String deviceId) {
    int deletedRows = jdbc.update("""
    DELETE FROM control_plane.devices
    WHERE device_id = ?
    """, deviceId);
    log.debug("Deleted {} rows for device: {}", deletedRows, deviceId);
    return deletedRows;
  }

  public int deleteSiteById(String siteId) {
    int deletedRows = jdbc.update("""
    DELETE FROM control_plane.sites
    WHERE site_id = ?
    """, siteId);
    log.debug("Deleted {} rows for site: {}", deletedRows, siteId);
    return deletedRows;
  }

  public int deleteSubscriptionById(String tenantId) {
    int deletedRows = jdbc.update("""
    DELETE FROM control_plane.subscriptions
    WHERE tenant_id = ?
    """, tenantId);
    log.debug("Deleted {} rows for subscription: {}", deletedRows, tenantId);
    return deletedRows;
  }

  public int deleteTenantById(String tenantId) {
    int deletedRows = jdbc.update("""
    DELETE FROM control_plane.tenants
    WHERE tenant_id = ?
    """, tenantId);
    log.debug("Deleted {} rows for tenant: {}", deletedRows, tenantId);
    return deletedRows;
  }

  /**
   * Dato un deviceId, ritorni tenantId e siteId.
   *
   * questa lancia eccezione se non trova nulla (EmptyResultDataAccessException)
   */
  public DeviceTenantSite findTenantAndSiteByDeviceId(String deviceId) {
    String sql = """
    SELECT tenant_id, site_id
    FROM control_plane.devices
    WHERE device_id = ?
    """;

    return jdbc.queryForObject(sql,
            (rs, rowNum) -> new DeviceTenantSite(
                    rs.getString("tenant_id"),
                    rs.getString("site_id")
            ),
            deviceId
    );
  }

  /**
   * Salva bootstrap token (hash + expiry) sul record device PENDING.
   *
   * INPUT:
   * - tokenHashHex: HMAC(tokenPlain)
   * - expiresAt: scadenza token
   *
   * NOTE DI SICUREZZA:
   * - tokenPlain NON è persistito, solo hash.
   * - Condizione status='PENDING' evita rigenerazione su device già attivi (policy L1).
   */
  public void setBootstrapToken(String deviceId, String tokenHashHex, Instant expiresAt) {
    jdbc.update("""
      UPDATE control_plane.devices
      SET bootstrap_token_hash = ?, bootstrap_expires_at = ?
      WHERE device_id = ? AND status = 'PENDING'
      """, tokenHashHex, Timestamp.from(expiresAt), deviceId);
  }

  public List<TenantResponse> findAllTenants() {
    return jdbc.query("""
      SELECT tenant_id, name, status, plan, created_at, updated_at
      FROM control_plane.tenants
      ORDER BY created_at DESC
      """,
      (rs, rowNum) -> new TenantResponse(
        rs.getString("tenant_id"),
        rs.getString("name"),
        rs.getString("status"),
        rs.getString("plan"),
        rs.getTimestamp("created_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant()
      )
    );
  }

  public List<SiteResponse> findSitesByTenant(String tenantId) {
    return jdbc.query("""
      SELECT site_id, tenant_id, name, timezone, status, created_at
      FROM control_plane.sites
      WHERE tenant_id = ?
      ORDER BY created_at DESC
      """,
      (rs, rowNum) -> new SiteResponse(
        rs.getString("site_id"),
        rs.getString("tenant_id"),
        rs.getString("name"),
        rs.getString("timezone"),
        rs.getString("status"),
        rs.getTimestamp("created_at").toInstant()
      ),
      tenantId
    );
  }

  public List<DeviceResponse> findDevicesBySite(String siteId) {
    return jdbc.query("""
      SELECT device_id, tenant_id, site_id, status, model, max_msgs_per_min, created_at
      FROM control_plane.devices
      WHERE site_id = ?
      ORDER BY created_at DESC
      """,
      (rs, rowNum) -> new DeviceResponse(
        rs.getString("device_id"),
        rs.getString("tenant_id"),
        rs.getString("site_id"),
        rs.getString("status"),
        rs.getString("model"),
        rs.getInt("max_msgs_per_min"),
        rs.getTimestamp("created_at").toInstant()
      ),
      siteId
    );
  }

  public record DeviceTenantSite(String tenantId, String siteId) {}
}