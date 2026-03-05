package ai.berticloud.admin.api;

import ai.berticloud.admin.api.dto.*;
import ai.berticloud.admin.db.AdminRepository;
import ai.berticloud.admin.security.BootstrapTokenIssuer;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/admin")
public class AdminController {
  private final AdminRepository repo;
  private final BootstrapTokenIssuer issuer;

  public AdminController(AdminRepository repo, BootstrapTokenIssuer issuer) {
    this.repo = repo;
    this.issuer = issuer;
  }

  @PostMapping("/tenants")
  public ResponseEntity<?> createTenant(@Valid @RequestBody CreateTenantRequest r) {
    repo.createTenant(r.tenantId(), r.name(), r.plan());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/subscriptions")
  public ResponseEntity<?> upsertSubscription(@Valid @RequestBody CreateSubscriptionRequest r) {
    repo.upsertSubscription(r.tenantId(), r.status(), r.validFrom(), r.validTo(), r.maxDevices());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/sites")
  public ResponseEntity<?> createSite(@Valid @RequestBody CreateSiteRequest r) {
    repo.createSite(r.siteId(), r.tenantId(), r.name(), r.timezone(), r.status());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/devices")
  public ResponseEntity<?> createDevice(@Valid @RequestBody CreateDeviceRequest r) {
    repo.createDevicePending(r.deviceId(), r.tenantId(), r.siteId(), r.model());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/devices/{deviceId}:bootstrapToken")
  public ResponseEntity<BootstrapTokenResponse> issueBootstrap(@PathVariable String deviceId) {
    var t = issuer.issueOneTimeToken(deviceId);
    repo.setBootstrapToken(deviceId, t.tokenHashHex(), t.expiresAt());
    return ResponseEntity.ok(new BootstrapTokenResponse(deviceId, t.tokenPlain(), t.expiresAt()));
  }
}