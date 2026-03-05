package ai.berticloud.enroll.security;

import ai.berticloud.enroll.util.CryptoUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Verifica bootstrap token (enrollment).
 *
 * MODELLO:
 * - admin-service ha salvato in DB: bootstrap_token_hash = HMAC(serverKey, tokenPlain)
 * - enrollment-service riceve tokenPlain (Bearer token) e ricalcola HMAC con la stessa serverKey.
 * - Confronto constant-time con hash DB.
 *
 * SECURITY:
 * - DB non contiene token plaintext, solo hash HMAC.
 * - Attaccante con DB dump NON può usarlo direttamente per enrollare.
 *
 * CONFIG:
 * - hmacKeyBase64 deve essere IDENTICO tra admin-service e enrollment-service.
 *   In prod: mettila in Secret Manager e inietta come env.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
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