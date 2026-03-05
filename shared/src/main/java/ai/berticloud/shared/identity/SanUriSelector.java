package ai.berticloud.shared.identity;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Selettore della SAN URI corretta quando una richiesta contiene più SAN.
 *
 * Perché serve:
 * - Un certificato può contenere più Subject Alternative Names (URI, DNS, IP, ecc.).
 * - Il Load Balancer può inviare tutte le SAN in un singolo header (comma-separated o whitespace-separated).
 *
 * RUOLO:
 * - Dato l'header client_cert_uri_sans (stringa),
 *   seleziona la SAN URI che segue la nostra convention URN "berticloudai".
 *
 * INPUT tipico:
 * - "urn:berticloudai:tenant:tnt-001:site:site-roma-001:device:rpi-123,urn:other:..."
 *
 * OUTPUT:
 * - La prima stringa che matcha il prefisso/sottostruttura attesa.
 *
 * NOTE:
 * - In L1 scegliamo "prima occorrenza valida".
 * - In L2 potresti rendere più rigida la scelta:
 *   - esigere esattamente 1 URN valida
 *   - rifiutare se ce ne sono più di una
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
public final class SanUriSelector {
  private SanUriSelector() {}

  /**
   * Estrae la URN principale dal valore header.
   *
   * @param headerValue valore dell'header che contiene 1+ SAN URI (separati da virgole/spazi).
   * @return la URN device trovata oppure null se assente.
   */
  public static String pickDeviceUrn(String headerValue) {
    if (headerValue == null || headerValue.isBlank()) return null;
    String[] parts = headerValue.split("[,\\s]+");
    for (String p : parts) {
      String s = p.trim();
      if (s.startsWith("urn:berticloudai:tenant:") && s.contains(":site:") && s.contains(":device:")) return s;
    }
    return null;
  }
}