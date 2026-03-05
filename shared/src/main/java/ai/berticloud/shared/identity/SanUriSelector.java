package ai.berticloud.shared.identity;

public final class SanUriSelector {
  private SanUriSelector() {}

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