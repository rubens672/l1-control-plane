package ai.berticloud.shared.security;

import java.util.Map;

public final class MtlsHeaders {
  private MtlsHeaders() {}

  public static final String PRESENT = "client_cert_present";
  public static final String VERIFIED = "client_cert_chain_verified";
  public static final String FP_SHA256 = "client_cert_sha256_fingerprint";
  public static final String URI_SANS = "client_cert_uri_sans";
  public static final String SUBJECT_DN = "client_cert_subject_dn";
  public static final String ISSUER_DN = "client_cert_issuer_dn";

  public static String getIgnoreCase(Map<String, String> headers, String name) {
    for (var e : headers.entrySet()) {
      if (e.getKey() != null && e.getKey().equalsIgnoreCase(name)) return e.getValue();
    }
    return null;
  }

  public static boolean isTrue(String v) {
    return v != null && (v.equalsIgnoreCase("true") || v.equalsIgnoreCase("1") || v.equalsIgnoreCase("yes"));
  }
}