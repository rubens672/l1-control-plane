package ai.berticloud.enroll.api;

import ai.berticloud.enroll.api.dto.SignCsrRequest;
import ai.berticloud.enroll.api.dto.SignCsrResponse;
import ai.berticloud.enroll.ca.CaMaterialLoader;
import ai.berticloud.enroll.ca.CsrAndCertService;
import ai.berticloud.enroll.db.EnrollmentRepository;
import ai.berticloud.enroll.security.BootstrapTokenService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Map;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * REST Controller per enrollment CSR-based.
 *
 * ENDPOINT:
 * - POST /v1/enrollments/{deviceId}:signCsr
 *
 * INPUT:
 * - Authorization: Bearer <bootstrapToken>  (one-time, TTL ~60m)
 * - Body JSON: { "csrPem": "-----BEGIN CERTIFICATE REQUEST-----..." }
 *
 * OUTPUT:
 * - 200 OK con:
 *   - clientCertPem: certificato client firmato (PEM)
 *   - chainPem: catena CA da installare sul device
 *
 * TRUST & SECURITY:
 * - Il bootstrap token è "bootstrap-only": abilita SOLO il signing CSR.
 * - Il token plaintext NON è in DB: DB contiene solo HMAC(token) + expiry.
 * - Il CSR è accettato solo se:
 *   1) device esiste e status = PENDING
 *   2) token valido e non scaduto
 *   3) CSR contiene SAN URI conforme e coerente con device registrato:
 *        - deviceId path == SAN.deviceId
 *        - tenantId/siteId SAN == tenantId/siteId in DB
 *
 * CONCURRENCY & CONSISTENCY:
 * - Leggiamo la riga device con SELECT ... FOR UPDATE per prevenire doppio enrollment:
 *   - impedisce race condition se due richieste arrivano quasi insieme
 *   - garantisce atomicità quando azzeriamo il token e attiviamo il device
 *
 * ERRORI:
 * - 401: token mancante/invalid/expired oppure bootstrap non impostato in DB
 * - 404: device non trovato
 * - 409: device non in stato PENDING (già attivo o revocato)
 * - 400: CSR malformato o SAN mismatch (tentativo spoof)
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 4 March 2026
 */
@RestController
@RequestMapping("/v1/enrollments")
public class EnrollmentController {
  private final EnrollmentRepository repo;
  private final BootstrapTokenService tokenSvc;
  private final CaMaterialLoader caLoader;
  private final CsrAndCertService csrSvc;

  public EnrollmentController(EnrollmentRepository repo,
                              BootstrapTokenService tokenSvc,
                              CaMaterialLoader caLoader,
                              CsrAndCertService csrSvc) {
    this.repo = repo;
    this.tokenSvc = tokenSvc;
    this.caLoader = caLoader;
    this.csrSvc = csrSvc;
  }

  @PostMapping("/{deviceId}:signCsr")
  @Transactional
  public ResponseEntity<?> signCsr(@PathVariable String deviceId,
                                  @RequestHeader(name="Authorization", required=false) String authz,
                                  @Valid @RequestBody SignCsrRequest req) throws Exception{

    // 1) Estrai Bearer token (bootstrap secret temporaneo)
    String token = extractBearer(authz);
    if (token == null) return ResponseEntity.status(401).body(Map.of("error","missing_bearer"));

    // 2) Carica device row con lock (FOR UPDATE) per rendere l'enrollment atomic e non duplicabile.
    var rowOpt = repo.findDeviceForEnrollForUpdate(deviceId);
    if (rowOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error","device_not_found"));

    // 3) Enroll ammesso solo se device è PENDING
    var row = rowOpt.get();
    if (!"PENDING".equals(row.status())) return ResponseEntity.status(409).body(Map.of("error","device_not_pending"));

    // 4) Token deve essere presente e non scaduto (bootstrap token è one-time e breve)
    if (row.bootstrapHash() == null || row.bootstrapExpiresAt() == null)
      return ResponseEntity.status(401).body(Map.of("error","bootstrap_not_set"));
    if (Instant.now().isAfter(row.bootstrapExpiresAt()))
      return ResponseEntity.status(401).body(Map.of("error","bootstrap_expired"));

    // 5) Verifica token: calcolo HMAC(token) e confronto constant-time con hash DB.
    if (!tokenSvc.verify(token, row.bootstrapHash()))
      return ResponseEntity.status(401).body(Map.of("error","bootstrap_invalid"));

    // 6) Parse CSR e valida SAN:
    //    - deviceId nel path deve combaciare col SAN.deviceId (anti spoof)
    // CSR parse + SAN validate (deviceId path coerente col SAN)
    var parsed = csrSvc.parseAndValidateCsr(req.csrPem(), deviceId);

    // 7) Guardrail: tenant/site del SAN devono combaciare con DB.
    //    Impedisce che qualcuno usi un token di un device per ottenere cert per un tenant/site diverso.
    // ulteriore guardrail: il SAN deve combaciare con tenant/site/device registrati
    if (!parsed.identity().tenantId().equals(row.tenantId()) || !parsed.identity().siteId().equals(row.siteId())) {
      return ResponseEntity.badRequest().body(Map.of("error","san_tenant_site_mismatch"));
    }

    // 8) Carica materiale CA da Secret Manager e firma cert client (EKU clientAuth + SAN copiato dal CSR).
    var ca = caLoader.load();
    var signed = csrSvc.sign(ca, parsed);

    // 9) Persistenza: aggiorna device e inserisce history (in transazione).
    X509Certificate cert = signed.cert();
    String issuerDn = cert.getIssuerX500Principal().getName();
    String subjectDn = cert.getSubjectX500Principal().getName();

    repo.activateAndStoreCert(
        deviceId,
        signed.fingerprintSha256Hex(),
        signed.certSerialHex(),
        signed.notAfter(),
        issuerDn,
        subjectDn
    );

    // 10) Response: cert PEM + chain PEM (il device li installerà per chiamare ingest con mTLS).
    String certPem = pemEncode(cert);
    String chainPem = ca.chainPem();

    return ResponseEntity.ok(new SignCsrResponse(certPem, chainPem));
  }

  private static String extractBearer(String authz) {
    if (authz == null) return null;
    String s = authz.trim();
    if (!s.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
    return s.substring(7).trim();
  }

  private static String pemEncode(X509Certificate cert) {
    try {
      String b64 = java.util.Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded());
      return "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}