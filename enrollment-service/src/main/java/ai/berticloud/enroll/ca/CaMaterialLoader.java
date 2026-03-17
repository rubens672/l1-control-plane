package ai.berticloud.enroll.ca;

import com.google.cloud.spring.secretmanager.SecretManagerTemplate;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Carica il materiale CA da Secret Manager.
 *
 * INPUT (Secret Manager):
 * - issuingKeySecret: PEM della private key della issuing CA
 * - issuingCertSecret: PEM del certificato della issuing CA
 * - chainSecret (opzionale): PEM della chain (intermediate/root). Se assente
 * usa issuingCert.
 *
 * PERCHÉ Secret Manager:
 * - Chiave CA è un segreto ad alta criticità.
 * - Evitiamo file nel container e riduciamo rischio leak.
 *
 * NOTE OPERATIVE:
 * - Questo loader è chiamato per ogni richiesta di enrollment nel codice L1.
 * In L2 puoi aggiungere caching in memoria del materiale CA (con TTL/rotation).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 202
 */
@Service
public class CaMaterialLoader {
  private final SecretManagerTemplate sm;
  private final String keySecret;
  private final String certSecret;
  private final String chainSecret;

  public CaMaterialLoader(SecretManagerTemplate sm,
      @Value("${app.ca.issuingKeySecret}") String keySecret,
      @Value("${app.ca.issuingCertSecret}") String certSecret,
      @Value("${app.ca.chainSecret:}") String chainSecret) {
    this.sm = sm;
    this.keySecret = keySecret;
    this.certSecret = certSecret;
    this.chainSecret = chainSecret;
  }

  /**
   * Carica key/cert/chain dal Secret Manager e li converte in oggetti Java.
   */
  public CaMaterial load() {
    //String keyPem = sm.getSecretString(keySecret);
    //String certPem = sm.getSecretString(certSecret);
    //String chainPem = (chainSecret == null || chainSecret.isBlank()) ? certPem : sm.getSecretString(chainSecret);

    PrivateKey key = parsePrivateKey(keySecret);
    X509Certificate cert = parseCert(certSecret);
    return new CaMaterial(key, cert, null);
  }

  /**
   * Parse PEM private key.
   * Supporta:
   * - PKCS#8 (PrivateKeyInfo)
   * - PEMKeyPair (formati classici OpenSSL)
   */
  private static PrivateKey parsePrivateKey(String pem) {
    try (PEMParser p = new PEMParser(new StringReader(pem))) {
      Object o = p.readObject();
      var conv = new JcaPEMKeyConverter();
      // gestisce PKCS#8 (PrivateKeyInfo) e altri formati comuni
      if (o instanceof org.bouncycastle.asn1.pkcs.PrivateKeyInfo info)
        return conv.getPrivateKey(info);
      if (o instanceof org.bouncycastle.openssl.PEMKeyPair kp)
        return conv.getKeyPair(kp).getPrivate();
      throw new IllegalArgumentException("Unsupported key PEM type: " + o.getClass());
    } catch (Exception e) {
      throw new RuntimeException("Cannot parse CA private key", e);
    }
  }

  /**
   * Parse PEM certificate in X509Certificate.
   */
  private static X509Certificate parseCert(String pem) {
    try (PEMParser p = new PEMParser(new StringReader(pem))) {
      Object o = p.readObject();
      if (!(o instanceof X509CertificateHolder h))
        throw new IllegalArgumentException("Invalid cert PEM");
      return new JcaX509CertificateConverter().getCertificate(h);
    } catch (Exception e) {
      throw new RuntimeException("Cannot parse CA certificate", e);
    }
  }
}