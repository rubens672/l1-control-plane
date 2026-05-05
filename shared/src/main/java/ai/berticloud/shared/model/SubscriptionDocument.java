package ai.berticloud.shared.model;

import java.time.Instant;

public class SubscriptionDocument {
    private String status; // 'ACTIVE', 'CANCELED', etc.
    private Instant validFrom;
    private Instant validTo;
    private int maxDevices;
    private Instant updatedAt;

    public SubscriptionDocument() {
    }

    public SubscriptionDocument(String status, Instant validFrom, Instant validTo, int maxDevices, Instant updatedAt) {
        this.status = status;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.maxDevices = maxDevices;
        this.updatedAt = updatedAt;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getValidFrom() { return validFrom; }
    public void setValidFrom(Instant validFrom) { this.validFrom = validFrom; }
    public Instant getValidTo() { return validTo; }
    public void setValidTo(Instant validTo) { this.validTo = validTo; }
    public int getMaxDevices() { return maxDevices; }
    public void setMaxDevices(int maxDevices) { this.maxDevices = maxDevices; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
