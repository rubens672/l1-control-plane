package ai.berticloud.shared.model;

import java.time.Instant;

public class DeviceCertHistoryDocument {
    private String fingerprintSha256;
    private String certSerial;
    private Instant notBefore;
    private Instant notAfter;
    private Instant issuedAt;
    private Instant revokedAt;
    private String revokeReason;

    public DeviceCertHistoryDocument() {
    }

    public DeviceCertHistoryDocument(String fingerprintSha256, String certSerial, Instant notBefore, Instant notAfter, Instant issuedAt) {
        this.fingerprintSha256 = fingerprintSha256;
        this.certSerial = certSerial;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
        this.issuedAt = issuedAt;
    }

    public String getFingerprintSha256() { return fingerprintSha256; }
    public void setFingerprintSha256(String fingerprintSha256) { this.fingerprintSha256 = fingerprintSha256; }
    public String getCertSerial() { return certSerial; }
    public void setCertSerial(String certSerial) { this.certSerial = certSerial; }
    public Instant getNotBefore() { return notBefore; }
    public void setNotBefore(Instant notBefore) { this.notBefore = notBefore; }
    public Instant getNotAfter() { return notAfter; }
    public void setNotAfter(Instant notAfter) { this.notAfter = notAfter; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
}
