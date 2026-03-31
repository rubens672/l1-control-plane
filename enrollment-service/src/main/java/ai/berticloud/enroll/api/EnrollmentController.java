package ai.berticloud.enroll.api;

import ai.berticloud.enroll.api.dto.SignCsrRequest;
import ai.berticloud.enroll.api.dto.SignCsrResponse;
import ai.berticloud.enroll.ca.CsrValidator;
import ai.berticloud.enroll.ca.issuer.CertificateIssuer;
import ai.berticloud.enroll.ca.issuer.IssuedCertificate;
import ai.berticloud.enroll.db.EnrollmentRepository;
import ai.berticloud.enroll.security.BootstrapTokenService;
import ai.berticloud.enroll.util.CryptoUtil;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import java.io.StringReader;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger log = LoggerFactory.getLogger(EnrollmentController.class);

  private final EnrollmentRepository repo;
  private final BootstrapTokenService tokenSvc;
  private final CsrValidator csrValidator;
  private final CertificateIssuer certificateIssuer;

  public EnrollmentController(EnrollmentRepository repo,
                              BootstrapTokenService tokenSvc,
                              CsrValidator csrValidator,
                              CertificateIssuer certificateIssuer) {
    this.repo = repo;
    this.tokenSvc = tokenSvc;
    this.csrValidator = csrValidator;
    this.certificateIssuer = certificateIssuer;
  }

  @PostMapping("/{deviceId}:signCsr")
  @Transactional
  public ResponseEntity<?> signCsr(@PathVariable("deviceId") String deviceId,
                                  @RequestHeader(name="Authorization", required=false) String authz,
                                  @Valid @RequestBody SignCsrRequest req) {
    log.info("Received CSR sign request for device: {}", deviceId);
    try {
      // 1) Estrai Bearer token (bootstrap secret temporaneo)
      String token = extractBearer(authz);
      if (token == null) {
          log.warn("Missing bearer token for device: {}", deviceId);
          return ResponseEntity.status(401).body(Map.of("error", "missing_bearer"));
      }

      // 2) Carica device row con lock (FOR UPDATE) per rendere l'enrollment atomic e non duplicabile.
      var rowOpt = repo.findDeviceForEnrollForUpdate(deviceId);
      if (rowOpt.isEmpty()) {
          log.warn("Device not found: {}", deviceId);
          return ResponseEntity.status(404).body(Map.of("error", "device_not_found"));
      }

      // 3) Enroll ammesso solo se device è PENDING
      var row = rowOpt.get();
      if (!"PENDING".equals(row.status())) {
          log.warn("Device is not PENDING: {}", deviceId);
          return ResponseEntity.status(409).body(Map.of("error", "device_not_pending"));
      }

      // 4) Token deve essere presente e non scaduto (bootstrap token è one-time e breve)
      if (row.bootstrapHash() == null || row.bootstrapExpiresAt() == null) {
          log.warn("Bootstrap token not set for device: {}", deviceId);
          return ResponseEntity.status(401).body(Map.of("error", "bootstrap_not_set"));
      }
      if (Instant.now().isAfter(row.bootstrapExpiresAt())) {
          log.warn("Bootstrap token expired for device: {}", deviceId);
          return ResponseEntity.status(401).body(Map.of("error", "bootstrap_expired"));
      }

      // 5) Verifica token: calcolo HMAC(token) e confronto constant-time con hash DB.
      if (!tokenSvc.verify(token, row.bootstrapHash())) {
          log.warn("Invalid bootstrap token for device: {}", deviceId);
          return ResponseEntity.status(401).body(Map.of("error", "bootstrap_invalid"));
      }

      // 6) Parse CSR e valida SAN:
      //    - deviceId nel path deve combaciare col SAN.deviceId (anti spoof)
      var parsed = csrValidator.parseAndValidateCsr(req.csrPem(), deviceId);

      // 7) Guardrail: tenant/site del SAN devono combaciare con DB.
      if (!parsed.identity().tenantId().equals(row.tenantId()) || !parsed.identity().siteId().equals(row.siteId())) {
        log.warn("SAN tenant/site mismatch for device: {}. Expected: {}/{}, Got: {}/{}", deviceId, row.tenantId(), row.siteId(), parsed.identity().tenantId(), parsed.identity().siteId());
        return ResponseEntity.badRequest().body(Map.of("error", "san_tenant_site_mismatch"));
      }

      // 8) Invia richiesta a GCP CAS per firma certificato client (EKU clientAuth + SAN copiato dal CSR).
      IssuedCertificate issued = certificateIssuer.issueDeviceCertificate(deviceId, req.csrPem(), parsed.urn());

      // 9) Persistenza: aggiorna device e inserisce history (in transazione).
      // Parse PEM for extracting Fingerprint, Issuer, Subject fields
      X509Certificate cert = parsePemCert(issued.clientCertPem());
      String issuerDn = cert.getIssuerX500Principal().getName();
      String subjectDn = cert.getSubjectX500Principal().getName();
      String fpHex = CryptoUtil.sha256Hex(cert.getEncoded());

      repo.activateAndStoreCert(
              deviceId,
              fpHex,
              issued.serialNumber(),
              issued.notAfter(),
              issuerDn,
              subjectDn
      );

      // 10) Response: cert PEM + chain PEM (il device li installerà per chiamare ingest con mTLS).
      String certPem = issued.clientCertPem();
      String chainPem = issued.chainPem();
      
      log.info("Successfully enrolled device: {}", deviceId);
      return ResponseEntity.ok(new SignCsrResponse(certPem, chainPem));
    } catch (IllegalArgumentException e) {
      log.warn("Invalid CSR or request data for device {}: {}", deviceId, e.getMessage());
      return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    } catch (Exception e) {
      log.error("Error during CSR signing for device: {}", deviceId, e);
      return ResponseEntity.status(500).body(Map.of("error", "Internal server error during enrollment"));
    }
  }

  private static String extractBearer(String authz) {
    if (authz == null) return null;
    String s = authz.trim();
    if (!s.regionMatches(true, 0, "Bearer ", 0, 7)) return null;
    return s.substring(7).trim();
  }

  private X509Certificate parsePemCert(String pem) throws Exception {
    try (PEMParser p = new PEMParser(new StringReader(pem))) {
      Object o = p.readObject();
      if (!(o instanceof X509CertificateHolder h)) {
          throw new IllegalArgumentException("Invalid cert PEM");
      }
      return new JcaX509CertificateConverter().getCertificate(h);
    }
  }
}