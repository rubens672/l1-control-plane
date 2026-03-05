package ai.berticloud.ingest.api;

import ai.berticloud.ingest.auth.AuthzService;
import ai.berticloud.ingest.db.AuthzRepository;
import ai.berticloud.shared.identity.DeviceIdentity;
import ai.berticloud.shared.identity.SanUriParser;
import ai.berticloud.shared.identity.SanUriSelector;
import ai.berticloud.shared.security.MtlsHeaders;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.protobuf.ByteString;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class IngestController {
  private final AuthzService authz;
  private final AuthzRepository repo;
  private final PubSubTemplate pubSub;
  private final String topic;
  private final ObjectMapper om = new ObjectMapper();

  public IngestController(AuthzService authz, AuthzRepository repo, PubSubTemplate pubSub,
                          @org.springframework.beans.factory.annotation.Value("${app.pubsub.topic}") String topic) {
    this.authz = authz;
    this.repo = repo;
    this.pubSub = pubSub;
    this.topic = topic;
  }

  @PostMapping("/telemetry")
  public ResponseEntity<?> postTelemetry(@RequestHeader Map<String, String> headers,
                                        @RequestBody String body) throws Exception {

    boolean present = MtlsHeaders.isTrue(MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.PRESENT));
    boolean verified = MtlsHeaders.isTrue(MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.VERIFIED));
    if (!present || !verified) return ResponseEntity.status(401).body(Map.of("error","mtls_required"));

    String fp = MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.FP_SHA256);
    String sansHeader = MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.URI_SANS);

    String deviceUrn = SanUriSelector.pickDeviceUrn(sansHeader);
    DeviceIdentity id = SanUriParser.parse(deviceUrn);

    JsonNode root = om.readTree(body);

    // reject se body prova a “dichiarare” un device/site diverso dal SAN
    String bodyDeviceId = textOrNull(root.get("deviceId"));
    String bodySiteId = textOrNull(root.get("siteId"));
    if (bodyDeviceId != null && !bodyDeviceId.equals(id.deviceId()))
      return ResponseEntity.badRequest().body(Map.of("error","deviceId_mismatch"));
    if (bodySiteId != null && !bodySiteId.equals(id.siteId()))
      return ResponseEntity.badRequest().body(Map.of("error","siteId_mismatch"));

    String key = id.tenantId()+"|"+id.siteId()+"|"+id.deviceId();
    var ctx = authz.authorizeOrThrow(key, id.tenantId(), id.siteId(), id.deviceId(), fp);

    String eventType = textOrNull(root.get("eventType"));
    String schemaVersion = textOrNull(root.get("schemaVersion"));

    Map<String, String> attrs = new HashMap<>();
    attrs.put("tenantId", ctx.tenantId());
    attrs.put("siteId", ctx.siteId());
    attrs.put("deviceId", ctx.deviceId());
    if (eventType != null) attrs.put("eventType", eventType);
    if (schemaVersion != null) attrs.put("schemaVersion", schemaVersion);

    pubSub.publish(topic, com.google.pubsub.v1.PubsubMessage.newBuilder()
        .setData(ByteString.copyFrom(body, StandardCharsets.UTF_8))
        .putAllAttributes(attrs)
        .build());

    repo.touchLastSeen(ctx.deviceId());
    return ResponseEntity.accepted().build();
  }

  private static String textOrNull(JsonNode n) {
    return (n == null || n.isNull()) ? null : n.asText(null);
  }

  @ExceptionHandler(AuthzService.Forbidden.class)
  public ResponseEntity<?> forbidden(AuthzService.Forbidden ex) {
    return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> badRequest(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }
}