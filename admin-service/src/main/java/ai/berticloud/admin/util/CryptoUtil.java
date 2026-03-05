package ai.berticloud.admin.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Utility crittografiche (minime) per admin-service.
 *
 * Cosa fa qui:
 * - Genera bootstrap token random, URL-safe (Base64url).
 * - Calcola HMAC-SHA256 in HEX per memorizzare una "password-like" derivata dal token.
 *
 * Perché HMAC e non SHA256 semplice:
 * - Con SHA256(token) un attaccante potrebbe fare rainbow table / brute-force più facilmente.
 * - Con HMAC(token, secretKey) serve anche la secretKey server-side per verificare/attaccare.
 *
 * NOTE:
 * - In produzione la chiave HMAC deve essere custodita in modo sicuro (Secret Manager).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public final class CryptoUtil {
  private static final SecureRandom RND = new SecureRandom();
  private CryptoUtil() {}

  /** Genera un token casuale (bytes random) codificato Base64 URL-safe senza padding. */
  public static String randomTokenUrlSafe(int bytes) {
    byte[] b = new byte[bytes];
    RND.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  /**
   * HMAC SHA-256 (HEX lowercase) su un messaggio UTF-8.
   * Usato per hashare il bootstrap token prima di salvarlo su DB.
   */
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