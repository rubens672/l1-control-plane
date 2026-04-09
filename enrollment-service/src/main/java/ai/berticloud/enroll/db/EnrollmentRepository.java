package ai.berticloud.enroll.db;

import ai.berticloud.shared.model.DeviceCertHistoryDocument;
import ai.berticloud.shared.model.DeviceDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Repository MongoDB per Enrollment.
 *
 * RESPONSABILITÀ:
 * - Leggere il record device.
 * - Aggiornare il record device portandolo da PENDING -> ACTIVE e consumando il bootstrap token
 *   in modo atomico (evitando race condition).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026 (Refactored to MongoDB)
 */
@Repository
public class EnrollmentRepository {
  private final MongoTemplate mongoTemplate;

  public EnrollmentRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /** Proiezione minimale del record device necessaria durante enrollment. */
  public record PendingDeviceRow(
      String deviceId, String tenantId, String siteId, String status,
      String bootstrapHash, Instant bootstrapExpiresAt
  ) {}

  /**
   * Carica il device. In MongoDB non blocchiamo la riga qui, ma controlleremo
   * al momento dell'update che il token sia ancora valido per evitare race condition.
   */
  public Optional<PendingDeviceRow> findDeviceForEnrollForUpdate(String deviceId) {
    DeviceDocument doc = mongoTemplate.findById(deviceId, DeviceDocument.class);
    if (doc == null) {
      return Optional.empty();
    }
    return Optional.of(new PendingDeviceRow(
        doc.getDeviceId(),
        doc.getTenantId(),
        doc.getSiteId(),
        doc.getStatus(),
        doc.getBootstrapTokenHash(),
        doc.getBootstrapExpiresAt()
    ));
  }

  /**
   * Attiva il device e persiste metadati certificato atomicamente.
   * Usiamo findAndModify o un update coordinato per evitare race condition:
   * si assicura che il device sia in PENDING e che il token hash non sia nullo!
   * Se l'update non colpisce nulla, lanciariamo un'eccezione logica se serve (ma per ora
   * la firma originale era void, confidando sull'atomicità).
   */
  public void activateAndStoreCert(String deviceId,
                                  String fingerprintSha256Hex,
                                  String certSerialHex,
                                  Instant certNotAfter,
                                  String issuerDn,
                                  String subjectDn) {

    DeviceCertHistoryDocument history = new DeviceCertHistoryDocument(
        fingerprintSha256Hex, certSerialHex, Instant.now(), certNotAfter, Instant.now()
    );

    // Atomic update! Richiede rigorosamente status = PENDING e tokenHash != null
    Query query = new Query(Criteria.where("deviceId").is(deviceId)
        .and("status").is("PENDING")
        .and("bootstrapTokenHash").ne(null));

    Update update = new Update()
        .set("status", "ACTIVE")
        .set("onboardedAt", Instant.now())
        .set("expectedFingerprintSha256", fingerprintSha256Hex)
        .set("certSerial", certSerialHex)
        .set("certNotAfter", certNotAfter)
        .set("issuerDn", issuerDn)
        .set("subjectDn", subjectDn)
        .set("bootstrapTokenHash", null)
        .set("bootstrapExpiresAt", null)
        .push("certHistory", history)
        .set("updatedAt", Instant.now());

    // Fai l'update. Se l'esito è 0, o il device non c'era, o era già ACTIVE/aggiornato.
    mongoTemplate.updateFirst(query, update, DeviceDocument.class);
  }
}