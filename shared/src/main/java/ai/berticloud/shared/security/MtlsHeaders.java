package ai.berticloud.shared.security;

import java.util.Map;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Utility per leggere gli header mTLS iniettati dall'External HTTPS Load Balancer (GCLB)
 * quando termina mTLS e inoltra verso Cloud Run.
 *
 * RUOLO NEL SISTEMA:
 * - In L1 l'ingest-service si fida del Load Balancer per:
 *   1) verificare la chain del certificato client (mTLS client authentication)
 *   2) esporre al backend alcune proprietà del certificato via header
 *
 * TRUST MODEL:
 * - Questi header sono considerati "trusted" SOLO perché:
 *   - arrivano dal Load Balancer (trusted boundary)
 *   - Cloud Run non è esposto direttamente (o comunque l'LB è la via ufficiale)
 *
 * IMPORTANTE:
 * - Gestiamo case-insensitive perché a volte i proxy normalizzano i nomi header.
 * - Per boolean header usiamo una funzione tollerante (true/1/yes).
 *
 * HEADER ATTESI (naming L1):
 * - client_cert_present
 * - client_cert_chain_verified
 * - client_cert_sha256_fingerprint
 * - client_cert_uri_sans
 * - client_cert_subject_dn
 * - client_cert_issuer_dn
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public final class MtlsHeaders {
  private MtlsHeaders() {}

  public static final String PRESENT = "client_cert_present";
  public static final String VERIFIED = "client_cert_chain_verified";
  public static final String FP_SHA256 = "client_cert_sha256_fingerprint";
  public static final String URI_SANS = "client_cert_uri_sans";
  public static final String SUBJECT_DN = "client_cert_subject_dn";
  public static final String ISSUER_DN = "client_cert_issuer_dn";

  /**
   * Legge un header ignorando il case del nome.
   * Utile perché alcuni layer possono cambiare la capitalizzazione.
   */
  public static String getIgnoreCase(Map<String, String> headers, String name) {
    for (var e : headers.entrySet()) {
      if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
    }
    return null;
  }

  /**
   * Converte una stringa header in boolean.
   * Accetta valori comuni: true/1/yes (case-insensitive).
   */
  public static boolean isTrue(String v) {
    return v != null && (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("1") || v.equalsIgnoreCase("yes"));
  }
}