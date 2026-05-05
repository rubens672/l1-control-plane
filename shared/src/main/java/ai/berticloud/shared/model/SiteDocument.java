package ai.berticloud.shared.model;

import java.time.Instant;

public class SiteDocument {
    private String siteId;
    private String name;
    private String timezone;
    private String status; // 'ACTIVE', 'SUSPENDED'
    private Instant createdAt;

    public SiteDocument() {
    }

    public SiteDocument(String siteId, String name, String timezone, String status, Instant createdAt) {
        this.siteId = siteId;
        this.name = name;
        this.timezone = timezone;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getSiteId() { return siteId; }
    public void setSiteId(String siteId) { this.siteId = siteId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
