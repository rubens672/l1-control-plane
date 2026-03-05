package ai.berticloud.ingest.db;

import ai.berticloud.ingest.auth.DeviceAuthContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class AuthzRepository {
  private final JdbcTemplate jdbc;

  public AuthzRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

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

  public void touchLastSeen(String deviceId) {
    jdbc.update("UPDATE control_plane.devices SET last_seen_at = now() WHERE device_id = ?", deviceId);
  }
}