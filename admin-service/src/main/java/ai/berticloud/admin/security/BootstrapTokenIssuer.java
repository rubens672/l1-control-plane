package ai.berticloud.admin.security;

import ai.berticloud.admin.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Emettitore di bootstrap token (one-time) per enrollment CSR.
 *
 * PERCHÉ ESISTE:
 * - Il device non può avere credenziali GCP.
 * - Prima di avere un certificato mTLS, serve un meccanismo "bootstrap" semplice.
 * - Questo token abilita SOLO la chiamata di enrollment (POST signCsr) e scade presto.
 *
 * MODELLO DI SICUREZZA:
 * - token plaintext: generato random (URL-safe) e mostrato all'admin SOLO una volta.
 * - in DB non salviamo mai il token plaintext, ma solo:
 *   bootstrap_token_hash = HMAC_SHA256(serverKey, tokenPlain)
 * - L'enrollment-service ricalcola l'HMAC e confronta con quello salvato.
 *
 * PROPRIETÀ:
 * - TTL configurabile (es. 60 minuti).
 * - HMAC key deve essere segreta:
 *   - in L1: env var BOOTSTRAP_HMAC_KEY_BASE64
 *   - in L2: Secret Manager + rotation
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@Service
public class BootstrapTokenIssuer {
  private final byte[] hmacKey;
  private final long ttlMinutes;
  private final String enrollmentUrl;
/*
* comando per la creazione randomica della chiave:
* openssl rand -base64 32
 */
  public BootstrapTokenIssuer(
      @Value("${app.bootstrap.hmacKeyBase64}") String hmacKeyBase64,
      @Value("${app.bootstrap.tokenTtlMinutes}") long ttlMinutes,
      @Value("${app.bootstrap.enrollmentUrl}") String enrollmentUrl
  ) {
    System.out.println("hmacKeyBase64: " + hmacKeyBase64);
    System.out.println("enrollmentUrl: " + enrollmentUrl);
    this.hmacKey = Base64.getDecoder().decode(hmacKeyBase64);
    this.ttlMinutes = ttlMinutes;
    this.enrollmentUrl = enrollmentUrl;
  }

  /**
   * Genera un token one-time e i metadati da persistere in DB.
   *
   * NOTE:
   * - tokenPlain è quello che consegni al cliente.
   * - tokenHashHex è quello che salvi nel DB.
   */
  public IssuedToken issueOneTimeToken(String deviceId) {
    String token = CryptoUtil.randomTokenUrlSafe(32); // ~256 bit
    String hashHex = CryptoUtil.hmacSha256Hex(hmacKey, token);
    Instant exp = Instant.now().plus(ttlMinutes, ChronoUnit.MINUTES);
    return new IssuedToken(deviceId, token, hashHex, exp, enrollmentUrl);
  }

  /**
   * Bundle dei dati generati:
   * - tokenPlain: solo response admin (non persistito)
   * - tokenHashHex + expiresAt: persistiti sul record device
   */
  public record IssuedToken(String deviceId, String tokenPlain, String tokenHashHex, Instant expiresAt, String enrollmentUrl) {}
}