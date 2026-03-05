package ai.berticloud.ingest.auth;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Objects;

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

  private static String normalizeFp(String fp) {
    if (fp == null) return null;
    return fp.trim().toUpperCase().replace(":", "");
  }

  public static final class Forbidden extends RuntimeException {
    public Forbidden(String reason) { super(reason); }
  }
}