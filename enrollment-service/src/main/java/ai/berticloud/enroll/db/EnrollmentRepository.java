package ai.berticloud.enroll.db;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@Repository
public class EnrollmentRepository {
  private final JdbcTemplate jdbc;

  public EnrollmentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public record PendingDeviceRow(
      String deviceId, String tenantId, String siteId, String status,
      String bootstrapHash, Instant bootstrapExpiresAt
  ) {}

  private static final RowMapper<PendingDeviceRow> RM = (rs, i) -> new PendingDeviceRow(
      rs.getString("device_id"),
      rs.getString("tenant_id"),
      rs.getString("site_id"),
      rs.getString("status"),
      rs.getString("bootstrap_token_hash"),
      rs.getTimestamp("bootstrap_expires_at") == null ? null : rs.getTimestamp("bootstrap_expires_at").toInstant()
  );

  public Optional<PendingDeviceRow> findDeviceForEnrollForUpdate(String deviceId) {
    return jdbc.query("""
      SELECT device_id, tenant_id, site_id, status, bootstrap_token_hash, bootstrap_expires_at
      FROM control_plane.devices
      WHERE device_id = ?
      FOR UPDATE
      """, RM, deviceId).stream().findFirst();
  }

  @Transactional
  public void activateAndStoreCert(String deviceId,
                                  String fingerprintSha256Hex,
                                  String certSerialHex,
                                  Instant certNotAfter,
                                  String issuerDn,
                                  String subjectDn) {

    // update device
    jdbc.update("""
      UPDATE control_plane.devices
      SET status = 'ACTIVE',
          onboarded_at = now(),
          expected_fingerprint_sha256 = ?,
          cert_serial = ?,
          cert_not_after = ?,
          issuer_dn = ?,
          subject_dn = ?,
          bootstrap_token_hash = NULL,
          bootstrap_expires_at = NULL
      WHERE device_id = ?
      """, fingerprintSha256Hex, certSerialHex, Timestamp.from(certNotAfter), issuerDn, subjectDn, deviceId);

    // history
    jdbc.update("""
      INSERT INTO control_plane.device_cert_history(
        device_id, fingerprint_sha256, cert_serial, not_before, not_after, issued_at, revoked_at, reason
      )
      VALUES(?, ?, ?, now(), ?, now(), NULL, NULL)
      """, deviceId, fingerprintSha256Hex, certSerialHex, Timestamp.from(certNotAfter));
  }
}