package ai.berticloud.admin.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDeviceRequest(
    @NotBlank String deviceId,
    @NotBlank String tenantId,
    @NotBlank String siteId,
    String model
) {}