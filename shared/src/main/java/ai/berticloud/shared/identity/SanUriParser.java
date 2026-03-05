package ai.berticloud.shared.identity;

import java.util.regex.Pattern;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Parser della SAN URI "canonizzata" per identità device.
 *
 * FORMAT DEFINITIVO (L1):
 *   urn:berticloudai:tenant:<TENANT_ID>:site:<SITE_ID>:device:<DEVICE_ID>
 *
 * Esempio:
 *   urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123
 *
 * RUOLO:
 * - Trasforma una stringa URN proveniente da:
 *   - ingest-service: header client_cert_uri_sans (fornito dal LB)
 *   - enrollment-service: CSR SAN URI (fornito dal device)
 *   in un oggetto DeviceIdentity.
 *
 * SICUREZZA:
 * - Qui validiamo SOLO il formato sintattico.
 * - La validazione "autorizzativa" (device esiste e appartiene al tenant/site corretto)
 *   avviene altrove (DB check nel service).
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public final class SanUriParser {
  private SanUriParser() {}

  // regex semplice e robusta: segmenti senza ":".
  private static final Pattern P = Pattern.compile(
      "^urn:berticloudai:tenant:([^:]+):site:([^:]+):device:([^:]+)$"
  );

  /**
   * Parse della URN e validazione del formato.
   *
   * @throws IllegalArgumentException se la URN è null/blank o non matcha il formato atteso.
   */
  public static DeviceIdentity parse(String uri) {
    if (uri == null) throw new IllegalArgumentException("SAN URI missing");
    var m = P.matcher(uri.trim());
    if (!m.matches()) throw new IllegalArgumentException("Invalid SAN URI: " + uri);
    return new DeviceIdentity(m.group(1), m.group(2), m.group(3));
  }
}