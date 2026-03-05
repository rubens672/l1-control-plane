package ai.berticloud.enroll.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

public final class CryptoUtil {
  private CryptoUtil() {}

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

  public static boolean constantTimeEqualsHex(String a, String b) {
    if (a == null || b == null) return false;
    byte[] ba = a.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    byte[] bb = b.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    return MessageDigest.isEqual(ba, bb);
  }

  public static String sha256Hex(byte[] data) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return hex(md.digest(data));
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