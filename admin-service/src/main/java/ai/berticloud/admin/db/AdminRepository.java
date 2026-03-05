package ai.berticloud.admin.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class AdminRepository {
  private final JdbcTemplate jdbc;

  public AdminRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void createTenant(String tenantId, String name, String plan) {
    jdbc.update("""
      INSERT INTO control_plane.tenants(tenant_id, name, status, plan, created_at, updated_at)
      VALUES(?, ?, 'ACTIVE', ?, now(), now())
      """, tenantId, name, plan);
  }

  public void upsertSubscription(String tenantId, String status, Instant validFrom, Instant validTo, int maxDevices) {
    jdbc.update("""
      INSERT INTO control_plane.subscriptions(tenant_id, status, valid_from, valid_to, max_devices, features, updated_at)
      VALUES(?, ?, ?, ?, ?, '{}'::jsonb, now())
      ON CONFLICT (tenant_id)
      DO UPDATE SET status = EXCLUDED.status, valid_from = EXCLUDED.valid_from, valid_to = EXCLUDED.valid_to,
                    max_devices = EXCLUDED.max_devices, updated_at = now()
      """, tenantId, status, Timestamp.from(validFrom), Timestamp.from(validTo), maxDevices);
  }

  public void createSite(String siteId, String tenantId, String name, String timezone, String status) {
    jdbc.update("""
      INSERT INTO control_plane.sites(site_id, tenant_id, name, timezone, status, created_at)
      VALUES(?, ?, ?, ?, ?, now())
      """, siteId, tenantId, name, timezone, status == null ? "ACTIVE" : status);
  }

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

  public void setBootstrapToken(String deviceId, String tokenHashHex, Instant expiresAt) {
    jdbc.update("""
      UPDATE control_plane.devices
      SET bootstrap_token_hash = ?, bootstrap_expires_at = ?
      WHERE device_id = ? AND status = 'PENDING'
      """, tokenHashHex, Timestamp.from(expiresAt), deviceId);
  }
}