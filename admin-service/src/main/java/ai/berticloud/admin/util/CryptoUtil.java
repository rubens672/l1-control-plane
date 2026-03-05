package ai.berticloud.admin.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class CryptoUtil {
  private static final SecureRandom RND = new SecureRandom();
  private CryptoUtil() {}

  public static String randomTokenUrlSafe(int bytes) {
    byte[] b = new byte[bytes];
    RND.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  public static String hmacSha256Hex(byte[] key, String msg) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key, "HmacSHA256"));
      byte[] out = mac.doFinal(msg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return hex(out);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static String hex(byte[] b) {
    StringBuilder sb = new StringBuilder(b.length * 2);
    for (byte x : b) sb.append(String.format("%02x", x));
    return sb.toString();
  }
}