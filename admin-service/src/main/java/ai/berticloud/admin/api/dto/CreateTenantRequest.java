package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
    @NotBlank String tenantId,
    @NotBlank String name,
    String plan
) {}