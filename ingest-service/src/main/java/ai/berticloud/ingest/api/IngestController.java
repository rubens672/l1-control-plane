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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * REST endpoint principale dell'Ingest Service.
 *
 * ENDPOINT:
 * - POST /v1/telemetry
 *
 * SECURITY MODEL (L1):
 * - Questo endpoint accetta SOLO richieste con mTLS verificato dal Load Balancer.
 * - Non ci fidiamo di tenantId/siteId/deviceId nel body JSON.
 * - Deriviamo l'identità del device dal SAN URI del certificato:
 *     urn:berticloudai:tenant:<TENANT>:site:<SITE>:device:<DEVICE>
 *
 * INGRESSO:
 * - Headers mTLS (iniettati dal LB):
 *   - client_cert_present
 *   - client_cert_chain_verified
 *   - client_cert_sha256_fingerprint
 *   - client_cert_uri_sans
 * - Body: JSON telemetria (raw)
 *
 * USCITA:
 * - Pub/Sub publish (topic globale) con:
 *   - data = raw JSON body
 *   - attributes = tenantId, siteId, deviceId, eventType, schemaVersion
 *
 * REJECT RULES:
 * - 401: mTLS non presente o non verificato
 * - 400: SAN mancante/invalid, oppure mismatch tra body.deviceId/siteId e SAN
 * - 403: authz fallita (device non valido, fingerprint mismatch, subscription scaduta, ecc.)
 *
 * NOTE OPERATIVE:
 * - Il publish è "fire and forget" (pubSub.publish) in L1.
 * - last_seen_at viene aggiornato best-effort in Cloud SQL.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@RestController
@RequestMapping("/v1")
public class IngestController {
  private static final Logger log = LoggerFactory.getLogger(IngestController.class);

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
                                        @RequestBody String body) {
    try {
      log.debug("Received telemetry request");

      // 1) Guardrail mTLS: se LB non ha validato il client cert, non accettiamo la richiesta.
      boolean present = MtlsHeaders.isTrue(MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.PRESENT));
      boolean verified = MtlsHeaders.isTrue(MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.VERIFIED));
      if (!present || !verified) {
          log.warn("mTLS missing or not verified. Rejecting request.");
          return ResponseEntity.status(401).body(Map.of("error","mtls_required"));
      }

      // 2) Estraiamo fingerprint + SANs dal header (iniettati dal LB).
      String fp = MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.FP_SHA256);
      String sansHeader = MtlsHeaders.getIgnoreCase(headers, MtlsHeaders.URI_SANS);

      // 3) Seleziona la SAN URI "nostra" e parsala in tenant/site/device.
      String deviceUrn = SanUriSelector.pickDeviceUrn(sansHeader);
      DeviceIdentity id = SanUriParser.parse(deviceUrn);

      // 4) Parse JSON: serve per (a) reject mismatch e (b) estrarre eventType/schemaVersion per attributi Pub/Sub.
      JsonNode root = om.readTree(body);

      // 5) Reject mismatch: se il body dichiara deviceId/siteId e non combacia con SAN -> richiesta sospetta.
      String bodyDeviceId = textOrNull(root.get("deviceId"));
      String bodySiteId = textOrNull(root.get("siteId"));
      if (bodyDeviceId != null && !bodyDeviceId.equals(id.deviceId())) {
        log.warn("Device ID mismatch. Expected: {}, Got: {}", id.deviceId(), bodyDeviceId);
        return ResponseEntity.badRequest().body(Map.of("error","deviceId_mismatch"));
      }
      if (bodySiteId != null && !bodySiteId.equals(id.siteId())) {
        log.warn("Site ID mismatch. Expected: {}, Got: {}", id.siteId(), bodySiteId);
        return ResponseEntity.badRequest().body(Map.of("error","siteId_mismatch"));
      }

      // 6) AuthZ: cache -> DB join fallback. Qui si validano subscription/status/fingerprint.
      String key = id.tenantId()+"|"+id.siteId()+"|"+id.deviceId();
      var ctx = authz.authorizeOrThrow(key, id.tenantId(), id.siteId(), id.deviceId(), fp);

      // 7) Attributi Pub/Sub: tenant/site/device sempre dalla identity forte; eventType/schemaVersion dal payload.
      String eventType = textOrNull(root.get("eventType"));
      String schemaVersion = textOrNull(root.get("schemaVersion"));

      Map<String, String> attrs = new HashMap<>();
      attrs.put("tenantId", ctx.tenantId());
      attrs.put("siteId", ctx.siteId());
      attrs.put("deviceId", ctx.deviceId());
      if (eventType != null) attrs.put("eventType", eventType);
      if (schemaVersion != null) attrs.put("schemaVersion", schemaVersion);

      // 8) Publish su topic globale. Il consumer (Dataflow/BigQuery) userà attributes per routing/partitioning.
      pubSub.publish(topic, com.google.pubsub.v1.PubsubMessage.newBuilder()
          .setData(ByteString.copyFrom(body, StandardCharsets.UTF_8))
          .putAllAttributes(attrs)
          .build());

      // 9) Aggiorna last_seen: utile per dashboard e health. Best-effort (se fallisce non blocchiamo ingest).
      repo.touchLastSeen(ctx.deviceId());
      
      log.debug("Telemetry processed successfully for device: {}", ctx.deviceId());
      return ResponseEntity.accepted().build();

    } catch (AuthzService.Forbidden ex) {
      log.warn("Forbidden access: {}", ex.getMessage());
      return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
    } catch (IllegalArgumentException ex) {
      log.warn("Bad request: {}", ex.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    } catch (Exception e) {
      log.error("Internal server error during telemetry ingestion", e);
      return ResponseEntity.status(500).body(Map.of("error", "internal_server_error"));
    }
  }

  private static String textOrNull(JsonNode n) {
    return (n == null || n.isNull()) ? null : n.asText(null);
  }

  // @ExceptionHandler methods can be removed or kept as fallback; 
  // they handled exceptions thrown out of postTelemetry which we now catch inline.
  @ExceptionHandler(AuthzService.Forbidden.class)
  public ResponseEntity<?> forbidden(AuthzService.Forbidden ex) {
    log.warn("Forbidden access: {}", ex.getMessage());
    return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> badRequest(IllegalArgumentException ex) {
    log.warn("Bad request: {}", ex.getMessage());
    return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
  }
}