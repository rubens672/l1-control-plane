package ai.berticloud.shared.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "tenants")
public class TenantDocument {

    @Id
    private String tenantId;
    private String name;
    private String status; // 'ACTIVE', 'SUSPENDED', 'CLOSED'
    private String plan; // 'BASIC' etc
    private Instant createdAt;
    private Instant updatedAt;

    // Embedded Subscription
    private SubscriptionDocument subscription;

    // Embedded Sites
    private List<SiteDocument> sites = new ArrayList<>();

    public TenantDocument() {
    }

    public TenantDocument(String tenantId, String name, String status, String plan, Instant createdAt, Instant updatedAt) {
        this.tenantId = tenantId;
        this.name = name;
        this.status = status;
        this.plan = plan;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public SubscriptionDocument getSubscription() { return subscription; }
    public void setSubscription(SubscriptionDocument subscription) { this.subscription = subscription; }
    
    public List<SiteDocument> getSites() { return sites; }
    public void setSites(List<SiteDocument> sites) { this.sites = sites; }
}
