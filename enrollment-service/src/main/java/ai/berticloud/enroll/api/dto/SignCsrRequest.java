package ai.berticloud.enroll.api.dto;

import jakarta.validation.constraints.NotBlank;

public record SignCsrRequest(
    @NotBlank String csrPem
) {}