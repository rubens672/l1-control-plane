package ai.berticloud.enroll.security;

import ai.berticloud.enroll.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
public class BootstrapTokenService {
  private final byte[] hmacKey;

  public BootstrapTokenService(@Value("${app.bootstrap.hmacKeyBase64}") String base64) {
    this.hmacKey = Base64.getDecoder().decode(base64);
  }

  public String hash(String tokenPlain) {
    return CryptoUtil.hmacSha256Hex(hmacKey, tokenPlain);
  }

  public boolean verify(String tokenPlain, String expectedHashHex) {
    String got = hash(tokenPlain);
    return CryptoUtil.constantTimeEqualsHex(got, expectedHashHex);
  }
}