package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSiteRequest(
    @NotBlank String siteId,
    @NotBlank String tenantId,
    @NotBlank String name,
    String timezone,
    String status // ACTIVE
) {}