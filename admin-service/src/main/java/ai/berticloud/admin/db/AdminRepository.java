package ai.berticloud.admin.db;

import ai.berticloud.shared.model.DeviceDocument;
import ai.berticloud.shared.model.SiteDocument;
import ai.berticloud.shared.model.SubscriptionDocument;
import ai.berticloud.shared.model.TenantDocument;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import ai.berticloud.admin.api.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Repository MongoDB per il control-plane DB.
 *
 * RUOLO:
 * - Implementa le operazioni DB necessarie a L1:
 *   - create tenant
 *   - upsert subscription
 *   - create site
 *   - create device (PENDING)
 *   - set bootstrap token hash + expiry sul device
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026 (Refactored to MongoDB)
 */
@Repository
public class AdminRepository {
  private static final Logger log = LoggerFactory.getLogger(AdminRepository.class);

  private final MongoTemplate mongoTemplate;

  public AdminRepository(MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public void createTenant(String tenantId, String name, String plan) {
    TenantDocument doc = new TenantDocument(tenantId, name, "ACTIVE", plan, Instant.now(), Instant.now());
    mongoTemplate.save(doc);
  }

  public void upsertSubscription(String tenantId, String status, Instant validFrom, Instant validTo, int maxDevices) {
    SubscriptionDocument sub = new SubscriptionDocument(status, validFrom, validTo, maxDevices, Instant.now());
    Query query = new Query(Criteria.where("tenantId").is(tenantId));
    Update update = new Update().set("subscription", sub).set("updatedAt", Instant.now());
    mongoTemplate.upsert(query, update, TenantDocument.class);
  }

  public void createSite(String siteId, String tenantId, String name, String timezone, String status) {
    SiteDocument site = new SiteDocument(siteId, name, timezone, status == null ? "ACTIVE" : status, Instant.now());
    Query query = new Query(Criteria.where("tenantId").is(tenantId));
    Update update = new Update().push("sites", site).set("updatedAt", Instant.now());
    mongoTemplate.updateFirst(query, update, TenantDocument.class);
  }

  public void createDevicePending(String deviceId, String tenantId, String siteId, String model) {
    DeviceDocument doc = new DeviceDocument(deviceId, tenantId, siteId, "PENDING", model, Instant.now(), Instant.now());
    doc.setMaxMsgsPerMin(60);
    mongoTemplate.insert(doc);
  }

  public int deleteDeviceById(String deviceId) {
    Query query = new Query(Criteria.where("deviceId").is(deviceId));
    long deletedCount = mongoTemplate.remove(query, DeviceDocument.class).getDeletedCount();
    log.debug("Deleted {} rows for device: {}", deletedCount, deviceId);
    return (int) deletedCount;
  }

  public int deleteSiteById(String siteId) {
    Query query = new Query(Criteria.where("sites.siteId").is(siteId));
    Update update = new Update().pull("sites", Query.query(Criteria.where("siteId").is(siteId)));
    long updatedCount = mongoTemplate.updateMulti(query, update, TenantDocument.class).getModifiedCount();
    log.debug("Deleted {} rows for site: {}", updatedCount, siteId);
    return (int) updatedCount;
  }

  public int deleteSubscriptionById(String tenantId) {
    Query query = new Query(Criteria.where("tenantId").is(tenantId));
    Update update = new Update().set("subscription.status", "CANCELED").set("updatedAt", Instant.now());
    long modifiedCount = mongoTemplate.updateFirst(query, update, TenantDocument.class).getModifiedCount();
    log.debug("Canceled {} subscription for tenant: {}", modifiedCount, tenantId);
    return (int) modifiedCount;
  }

  public int deleteTenantById(String tenantId) {
    Query query = new Query(Criteria.where("tenantId").is(tenantId));
    Update update = new Update().set("status", "INACTIVE").set("updatedAt", Instant.now());
    long modifiedCount = mongoTemplate.updateFirst(query, update, TenantDocument.class).getModifiedCount();
    log.debug("Suspended {} tenant: {}", modifiedCount, tenantId);
    return (int) modifiedCount;
  }

  public DeviceTenantSite findTenantAndSiteByDeviceId(String deviceId) {
    Query query = new Query(Criteria.where("deviceId").is(deviceId));
    query.fields().include("tenantId", "siteId");
    DeviceDocument doc = mongoTemplate.findOne(query, DeviceDocument.class);
    if (doc == null) {
      throw new EmptyResultDataAccessException(1);
    }
    return new DeviceTenantSite(doc.getTenantId(), doc.getSiteId());
  }

  public void setBootstrapToken(String deviceId, String tokenHashHex, Instant expiresAt) {
    Query query = new Query(Criteria.where("deviceId").is(deviceId).and("status").is("PENDING"));
    Update update = new Update()
        .set("bootstrapTokenHash", tokenHashHex)
        .set("bootstrapExpiresAt", expiresAt)
        .set("updatedAt", Instant.now());
    mongoTemplate.updateFirst(query, update, DeviceDocument.class);
  }

  public List<TenantResponse> findAllTenants() {
    return mongoTemplate.findAll(TenantDocument.class).stream()
        .map(t -> new TenantResponse(
            t.getTenantId(),
            t.getName(),
            t.getStatus(),
            t.getPlan(),
            t.getCreatedAt(),
            t.getUpdatedAt()
        )).toList();
  }

  public List<SiteResponse> findSitesByTenant(String tenantId) {
    TenantDocument tenant = mongoTemplate.findById(tenantId, TenantDocument.class);
    if (tenant == null || tenant.getSites() == null) return Collections.emptyList();
    
    return tenant.getSites().stream().map(s -> new SiteResponse(
        s.getSiteId(),
        tenantId,
        s.getName(),
        s.getTimezone(),
        s.getStatus(),
        s.getCreatedAt()
    )).toList();
  }

  public List<DeviceResponse> findDevicesBySite(String siteId) {
    Query query = new Query(Criteria.where("siteId").is(siteId));
    return mongoTemplate.find(query, DeviceDocument.class).stream().map(d -> new DeviceResponse(
        d.getDeviceId(),
        d.getTenantId(),
        d.getSiteId(),
        d.getStatus(),
        d.getModel(),
        d.getMaxMsgsPerMin(),
        d.getCreatedAt()
    )).toList();
  }

  public record DeviceTenantSite(String tenantId, String siteId) {}
}