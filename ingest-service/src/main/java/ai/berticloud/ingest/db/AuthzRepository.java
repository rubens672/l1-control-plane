package ai.berticloud.ingest.db;

import ai.berticloud.ingest.auth.DeviceAuthContext;
import ai.berticloud.shared.model.DeviceDocument;
import ai.berticloud.shared.model.SiteDocument;
import ai.berticloud.shared.model.TenantDocument;
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
 * Accesso dati (MongoDB) per l'ingest-service.
 *
 * RUOLO:
 * - Carica dal control-plane DB il contesto autorizzativo (Tenant, Site, Subscription, Device).
 * - Aggiorna last_seen_at per health/monitoring e reporting "device alive".
 *
 * DESIGN:
 * - Le letture avvengono su Device e Tenant (che include aggregati Sub e Sites).
 * - Queste query multiple sono estremamente veloci rispetto a join e avvengono
 *   comunque solo su cache miss.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026 (Refactored to MongoDB)
 */
@Repository
public class AuthzRepository {
  private final MongoTemplate mongoTemplate;

  public AuthzRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  /**
   * Carica il contesto authz per un device (tenant/site/device) come "source of truth".
   */
  public Optional<DeviceAuthContext> loadAuthContext(String tenantId, String siteId, String deviceId) {
    // 1. Leggi dispositivo
    Query deviceQuery = new Query(Criteria.where("_id").is(deviceId)
        .and("tenantId").is(tenantId)
        .and("siteId").is(siteId));
    DeviceDocument device = mongoTemplate.findOne(deviceQuery, DeviceDocument.class);

    if (device == null) {
      return Optional.empty();
    }

    // 2. Leggi tenant (contiene subscription e sites)
    TenantDocument tenant = mongoTemplate.findById(tenantId, TenantDocument.class);

    if (tenant == null) {
      return Optional.empty();
    }

    // 3. Estrai sito
    String siteStatus = null;
    if (tenant.getSites() != null) {
      for (SiteDocument s : tenant.getSites()) {
        if (s.getSiteId().equals(siteId)) {
          siteStatus = s.getStatus();
          break;
        }
      }
    }
    if (siteStatus == null) return Optional.empty();

    // 4. Estrai subscription
    String subStatus = "INACTIVE";
    Instant validTo = null;
    if (tenant.getSubscription() != null) {
      subStatus = tenant.getSubscription().getStatus();
      validTo = tenant.getSubscription().getValidTo();
    }

    // Costruisci il costesto
    return Optional.of(new DeviceAuthContext(
        tenant.getTenantId(),
        siteId,
        device.getDeviceId(),
        tenant.getStatus(),
        siteStatus,
        device.getStatus(),
        subStatus,
        validTo,
        device.getExpectedFingerprintSha256(),
        device.getMaxMsgsPerMin()
    ));
  }

  /**
   * Aggiorna last_seen_at in modo best-effort.
   */
  public void touchLastSeen(String deviceId) {
    Query query = new Query(Criteria.where("_id").is(deviceId));
    Update update = new Update().set("lastSeenAt", Instant.now());
    mongoTemplate.updateFirst(query, update, DeviceDocument.class);
  }
}