package ai.berticloud.shared.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "devices")
public class DeviceDocument {

    @Id
    private String deviceId;

    @Indexed
    private String tenantId;

    @Indexed
    private String siteId;

    private String status; // 'PENDING', 'ACTIVE', 'SUSPENDED', 'REVOKED'
    private String model;
    private Instant onboardedAt;
    private Instant lastSeenAt;
    private int maxMsgsPerMin = 60;

    @Indexed
    private String expectedFingerprintSha256;
    private String certSerial;
    private Instant certNotAfter;
    private String issuerDn;
    private String subjectDn;

    private String bootstrapTokenHash;
    private Instant bootstrapExpiresAt;

    private Instant createdAt;
    private Instant updatedAt;

    private List<DeviceCertHistoryDocument> certHistory = new ArrayList<>();

    public DeviceDocument() {
    }

    public DeviceDocument(String deviceId, String tenantId, String siteId, String status, String model, Instant createdAt, Instant updatedAt) {
        this.deviceId = deviceId;
        this.tenantId = tenantId;
        this.siteId = siteId;
        this.status = status;
        this.model = model;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Instant getOnboardedAt() { return onboardedAt; }
    public void setOnboardedAt(Instant onboardedAt) { this.onboardedAt = onboardedAt; }
    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public int getMaxMsgsPerMin() { return maxMsgsPerMin; }
    public void setMaxMsgsPerMin(int maxMsgsPerMin) { this.maxMsgsPerMin = maxMsgsPerMin; }
    public String getExpectedFingerprintSha256() { return expectedFingerprintSha256; }
    public void setExpectedFingerprintSha256(String expectedFingerprintSha256) { this.expectedFingerprintSha256 = expectedFingerprintSha256; }
    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }
    public Instant getCertNotAfter() { return certNotAfter; }
    public void setCertNotAfter(Instant certNotAfter) { this.certNotAfter = certNotAfter; }
    public String getIssuerDn() { return issuerDn; }
    public void setIssuerDn(String issuerDn) { this.issuerDn = issuerDn; }
    public String getSubjectDn() { return subjectDn; }
    public void setSubjectDn(String subjectDn) { this.subjectDn = subjectDn; }
    public String getBootstrapTokenHash() { return bootstrapTokenHash; }
    public void setBootstrapTokenHash(String bootstrapTokenHash) { this.bootstrapTokenHash = bootstrapTokenHash; }
    public Instant getBootstrapExpiresAt() { return bootstrapExpiresAt; }
    public void setBootstrapExpiresAt(Instant bootstrapExpiresAt) { this.bootstrapExpiresAt = bootstrapExpiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public List<DeviceCertHistoryDocument> getCertHistory() { return certHistory; }
    public void setCertHistory(List<DeviceCertHistoryDocument> certHistory) { this.certHistory = certHistory; }
}
