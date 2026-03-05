package ai.berticloud.admin.security;

import ai.berticloud.admin.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class BootstrapTokenIssuer {
  private final byte[] hmacKey;
  private final long ttlMinutes;

  public BootstrapTokenIssuer(
      @Value("${app.bootstrap.hmacKeyBase64}") String hmacKeyBase64,
      @Value("${app.bootstrap.tokenTtlMinutes}") long ttlMinutes
  ) {
    this.hmacKey = Base64.getDecoder().decode(hmacKeyBase64);
    this.ttlMinutes = ttlMinutes;
  }

  public IssuedToken issueOneTimeToken(String deviceId) {
    String token = CryptoUtil.randomTokenUrlSafe(32); // ~256 bit
    String hashHex = CryptoUtil.hmacSha256Hex(hmacKey, token);
    Instant exp = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
    return new IssuedToken(deviceId, token, hashHex, exp);
  }

  public record IssuedToken(String deviceId, String tokenPlain, String tokenHashHex, Instant expiresAt) {}
}