package ai.berticloud.ingest.auth;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Motore di autorizzazione (AuthZ) per l'ingest.
 *
 * TRUST BOUNDARY (IMPORTANTISSIMO):
 * - L'identità del device (tenantId/siteId/deviceId) è derivata SOLO dal SAN URI del certificato mTLS.
 * - Il Load Balancer valida il certificato (chain) e passa fingerprint + SAN al backend.
 * - Il body JSON non è trusted per identity (può essere spoofato).
 *
 * RESPONSABILITÀ:
 * - Rispondere velocemente alla domanda: "questo device può pubblicare telemetria ora?"
 * - Applicare policy L1:
 *   - tenant/site/device ACTIVE
 *   - subscription ACTIVE e non scaduta
 *   - fingerprint mTLS == expectedFingerprintSha256 in DB
 *   - (in futuro) rate limit maxMsgsPerMin
 *
 * PERFORMANCE:
 * - 2 livelli cache:
 *   1) denyCache: blocca rapidamente device noti come invalidi per breve periodo
 *   2) allowCache: evita query DB ripetute per device validi (TTL minuti)
 *
 * ERRORI:
 * - Lancia Forbidden(reason) -> mapping 403 dal controller.
 * - Il controller gestisce 401 quando mTLS non è presente/validato.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@Service
public class AuthzService {
  private final Cache allow;
  private final Cache deny;
  private final ai.berticloud.ingest.db.AuthzRepository repo;

  public AuthzService(CacheManager cacheManager, ai.berticloud.ingest.db.AuthzRepository repo) {
    this.allow = Objects.requireNonNull(cacheManager.getCache("deviceAuthContext"));
    this.deny = Objects.requireNonNull(cacheManager.getCache("denyCache"));
    this.repo = repo;
  }

  /**
   * Decide se un device è autorizzato.
   *
   * @param key      chiave cache composta: tenant|site|device
   * @param tenantId dal SAN
   * @param siteId   dal SAN
   * @param deviceId dal SAN
   * @param fpSha256 fingerprint dal mTLS header (LB)
   */
  public DeviceAuthContext authorizeOrThrow(String key, String tenantId, String siteId, String deviceId, String fpSha256) {
    Boolean denied = deny.get(key, Boolean.class);
    if (Boolean.TRUE.equals(denied)) throw new Forbidden("deny_cached");

    DeviceAuthContext ctx = allow.get(key, DeviceAuthContext.class);
    if (ctx == null) {
      var loaded = repo.loadAuthContext(tenantId, siteId, deviceId);
      if (loaded.isEmpty()) { deny.put(key, true); throw new Forbidden("device_not_found"); }
      ctx = loaded.get();
      allow.put(key, ctx);
    }

    if (!"ACTIVE".equals(ctx.tenantStatus())) { deny.put(key,true); throw new Forbidden("tenant_not_active"); }
    if (!"ACTIVE".equals(ctx.subscriptionStatus())) { deny.put(key,true); throw new Forbidden("sub_not_active"); }
    if (Instant.now().isAfter(ctx.subscriptionValidTo())) { deny.put(key,true); throw new Forbidden("sub_expired"); }
    if (!"ACTIVE".equals(ctx.siteStatus())) { deny.put(key,true); throw new Forbidden("site_not_active"); }
    if (!"ACTIVE".equals(ctx.deviceStatus())) { deny.put(key,true); throw new Forbidden("device_not_active"); }

    String expected = normalizeFp(ctx.expectedFingerprintSha256());
    String got = normalizeFp(fpSha256);
    if (expected == null || got == null || !expected.equals(got)) {
      deny.put(key,true);
      throw new Forbidden("fingerprint_mismatch");
    }

    return ctx;
  }

  /**
   * Normalizza fingerprint:
   * - alcuni sistemi includono ":" nel fingerprint, altri no
   * - normalizziamo a HEX uppercase senza ":" per confronto robusto
   */
  private static String normalizeFp(String fp) {
    if (fp == null) return null;
    return fp.trim().toUpperCase().replace(":", "");
  }

  /** Eccezione di dominio: usata dal controller per rispondere 403 con reason machine-readable. */
  public static final class Forbidden extends RuntimeException {
    public Forbidden(String reason) { super(reason); }
  }
}