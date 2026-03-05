package ai.berticloud.ingest.db;

import ai.berticloud.ingest.auth.DeviceAuthContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Accesso dati Cloud SQL (Postgres) per l'ingest-service.
 *
 * RUOLO:
 * - Carica dal control-plane DB il contesto autorizzativo (join tenant/site/subscription/device).
 * - Aggiorna last_seen_at per health/monitoring e reporting "device alive".
 *
 * DESIGN:
 * - Una singola query "join" produce DeviceAuthContext.
 * - Questa query viene usata SOLO su cache miss (hot-path ottimizzato).
 *
 * NOTE:
 * - Schema atteso: control_plane.devices/tenants/sites/subscriptions
 * - In L1 usiamo JdbcTemplate per semplicità (niente JPA).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@Repository
public class AuthzRepository {
  private final JdbcTemplate jdbc;

  public AuthzRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /**
   * Carica il contesto authz per un device (tenant/site/device) come "source of truth".
   *
   * IMPORTANT:
   * - Noi riceviamo tenant/site/device dal SAN del certificato (trusted boundary).
   * - Qui verifichiamo che esista una riga device coerente con quell'identità.
   */
  public Optional<DeviceAuthContext> loadAuthContext(String tenantId, String siteId, String deviceId) {
    String sql = """
      SELECT
        d.device_id,
        d.tenant_id,
        d.site_id,
        t.status AS tenant_status,
        s.status AS site_status,
        d.status AS device_status,
        sub.status AS subscription_status,
        sub.valid_to AS subscription_valid_to,
        d.expected_fingerprint_sha256,
        d.max_msgs_per_min
      FROM control_plane.devices d
      JOIN control_plane.tenants t ON t.tenant_id = d.tenant_id
      JOIN control_plane.sites s ON s.site_id = d.site_id
      JOIN control_plane.subscriptions sub ON sub.tenant_id = d.tenant_id
      WHERE d.device_id = ? AND d.tenant_id = ? AND d.site_id = ?
      """;

    return jdbc.query(sql, rs -> {
      if (!rs.next()) return Optional.empty();
      Instant validTo = rs.getTimestamp("subscription_valid_to").toInstant();
      return Optional.of(new DeviceAuthContext(
          rs.getString("tenant_id"),
          rs.getString("site_id"),
          rs.getString("device_id"),
          rs.getString("tenant_status"),
          rs.getString("site_status"),
          rs.getString("device_status"),
          rs.getString("subscription_status"),
          validTo,
          rs.getString("expected_fingerprint_sha256"),
          rs.getInt("max_msgs_per_min")
      ));
    }, deviceId, tenantId, siteId);
  }

  /**
   * Aggiorna last_seen_at in modo best-effort.
   * Serve a:
   * - dashboard cliente
   * - troubleshooting
   * - SLA/monitoring base
   */
  public void touchLastSeen(String deviceId) {
    jdbc.update("UPDATE control_plane.devices SET last_seen_at = now() WHERE device_id = ?", deviceId);
  }
}