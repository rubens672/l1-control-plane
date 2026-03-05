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

    String token = extractBearer(authz);
    if (token == null) return ResponseEntity.status(401).body(Map.of("error","missing_bearer"));

    var rowOpt = repo.findDeviceForEnrollForUpdate(deviceId);
    if (rowOpt.isEmpty()) return ResponseEntity.status(404).body(Map.of("error","device_not_found"));

    var row = rowOpt.get();
    if (!"PENDING".equals(row.status())) return ResponseEntity.status(409).body(Map.of("error","device_not_pending"));
    if (row.bootstrapHash() == null || row.bootstrapExpiresAt() == null)
      return ResponseEntity.status(401).body(Map.of("error","bootstrap_not_set"));
    if (Instant.now().isAfter(row.bootstrapExpiresAt()))
      return ResponseEntity.status(401).body(Map.of("error","bootstrap_expired"));

    if (!tokenSvc.verify(token, row.bootstrapHash()))
      return ResponseEntity.status(401).body(Map.of("error","bootstrap_invalid"));

    // CSR parse + SAN validate (deviceId path coerente col SAN)
    var parsed = csrSvc.parseAndValidateCsr(req.csrPem(), deviceId);

    // ulteriore guardrail: il SAN deve combaciare con tenant/site/device registrati
    if (!parsed.identity().tenantId().equals(row.tenantId()) || !parsed.identity().siteId().equals(row.siteId())) {
      return ResponseEntity.badRequest().body(Map.of("error","san_tenant_site_mismatch"));
    }

    var ca = caLoader.load();
    var signed = csrSvc.sign(ca, parsed);

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