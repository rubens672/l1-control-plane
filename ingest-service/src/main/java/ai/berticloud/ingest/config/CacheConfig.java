package ai.berticloud.ingest.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Configurazione Cache (Caffeine) per l'hot-path dell'ingest.
 *
 * OBIETTIVO:
 * - Ridurre le query su Cloud SQL durante il flusso di ingest telemetria,
 *   mantenendo un comportamento "production-oriented" e semplice.
 *
 * CACHE DEFINITE:
 * 1) deviceAuthContext (ALLOW CACHE)
 *    - contiene contesto authz precalcolato per un device:
 *      tenant/site/device status + subscription status/validity + expected fingerprint + rate limit
 *    - TTL: 5-15 min (accettiamo revoca "non immediata" in L1)
 *
 * 2) denyCache (NEGATIVE CACHE)
 *    - memoizza per poco tempo device non valido/inesistente/revocato
 *      per evitare query ripetute durante attacchi o misconfig.
 *    - TTL: breve (es. 90 sec)
 *
 * NOTE DI SICUREZZA:
 * - TTL non critico in L1: una revoca può propagare con ritardo (accettato).
 * - In L2 potrai ridurre TTL o introdurre invalidazione push/pull.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@Configuration
public class CacheConfig {

  @Bean
  public CacheManager cacheManager(
      @Value("${app.cache.allowTtlMinutes}") long allowTtlMinutes,
      @Value("${app.cache.denyTtlSeconds}") long denyTtlSeconds,
      @Value("${app.cache.maximumSize}") long maximumSize
  ) {
    CaffeineCache allow = new CaffeineCache("deviceAuthContext",
        Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(Duration.ofMinutes(allowTtlMinutes))
            .build());

    CaffeineCache deny = new CaffeineCache("denyCache",
        Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(Duration.ofSeconds(denyTtlSeconds))
            .build());

    SimpleCacheManager m = new SimpleCacheManager();
    m.setCaches(List.of(allow, deny));
    return m;
  }
}