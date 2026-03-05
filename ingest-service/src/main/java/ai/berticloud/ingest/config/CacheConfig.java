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