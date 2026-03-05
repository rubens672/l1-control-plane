package ai.berticloud.shared.identity;

import java.util.regex.Pattern;

public final class SanUriParser {
  private SanUriParser() {}

  private static final Pattern P = Pattern.compile(
      "^urn:berticloudai:tenant:([^:]+):site:([^:]+):device:([^:]+)$"
  );

  public static DeviceIdentity parse(String uri) {
    if (uri == null) throw new IllegalArgumentException("SAN URI missing");
    var m = P.matcher(uri.trim());
    if (!m.matches()) throw new IllegalArgumentException("Invalid SAN URI: " + uri);
    return new DeviceIdentity(m.group(1), m.group(2), m.group(3));
  }
}