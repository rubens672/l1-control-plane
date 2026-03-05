package ai.berticloud.enroll.api.dto;

public record SignCsrResponse(
    String clientCertPem,
    String chainPem
) {}