/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */
package ai.berticloud.webapp.client;

import ai.berticloud.webapp.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class AdminApiClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AdminApiClient(RestTemplate restTemplate, @Value("${app.admin-service.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public void createTenant(CreateTenantForm form) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("tenantId", "tnt-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        payload.put("name", form.getName());
        payload.put("plan", form.getPlan());
        restTemplate.postForEntity(baseUrl + "/tenants", payload, Void.class);
    }

    public List<TenantDto> listTenants() {
        ResponseEntity<List<TenantDto>> response = restTemplate.exchange(
                baseUrl + "/tenants",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<TenantDto>>() {}
        );
        return response.getBody();
    }

    public void createSubscription(CreateSubscriptionForm form) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("tenantId", form.getTenantId());
        payload.put("status", "ACTIVE");
        payload.put("validFrom", java.time.Instant.now().toString());
        payload.put("validTo", java.time.Instant.now()
                .plus(java.time.Duration.ofDays(form.getValidDays())).toString());
        payload.put("maxDevices", form.getMaxDevices());

        restTemplate.postForEntity(baseUrl + "/subscriptions", payload, Void.class);
    }

    public void createSite(CreateSiteForm form) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("siteId", "site-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        payload.put("tenantId", form.getTenantId());
        payload.put("name", form.getName());
        payload.put("timezone", form.getTimezone());
        payload.put("status", "ACTIVE");
        restTemplate.postForEntity(baseUrl + "/sites", payload, Void.class);
    }

    public List<SiteDto> listSitesByTenant(String tenantId) {
        ResponseEntity<List<SiteDto>> response = restTemplate.exchange(
                baseUrl + "/sites?tenantId=" + tenantId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<SiteDto>>() {}
        );
        return response.getBody();
    }

    public void createDevice(CreateDeviceForm form) {
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("deviceId", "dev-" + java.util.UUID.randomUUID().toString().substring(0, 8));
        payload.put("tenantId", form.getTenantId());
        payload.put("siteId", form.getSiteId());
        payload.put("model", form.getModel());
        restTemplate.postForEntity(baseUrl + "/devices", payload, Void.class);
    }

    public List<DeviceDto> listDevicesBySite(String siteId) {
        ResponseEntity<List<DeviceDto>> response = restTemplate.exchange(
                baseUrl + "/devices?siteId=" + siteId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<DeviceDto>>() {}
        );
        return response.getBody();
    }

    public BootstrapTokenResponse generateBootstrapToken(String deviceId) {
        ResponseEntity<BootstrapTokenResponse> response = restTemplate.postForEntity(
                baseUrl + "/devices/" + deviceId + ":bootstrapToken",
                null,
                BootstrapTokenResponse.class
        );
        return response.getBody();
    }

    public void deleteTenant(String tenantId) {
        restTemplate.delete(baseUrl + "/tenants/" + tenantId);
    }

    public void deleteSubscription(String tenantId) {
        restTemplate.delete(baseUrl + "/subscriptions/" + tenantId);
    }

    public void deleteSite(String siteId) {
        restTemplate.delete(baseUrl + "/sites/" + siteId);
    }

    public void deleteDevice(String deviceId) {
        restTemplate.delete(baseUrl + "/devices/" + deviceId);
    }
}
