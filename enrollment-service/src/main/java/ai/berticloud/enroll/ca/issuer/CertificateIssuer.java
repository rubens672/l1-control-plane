package ai.berticloud.enroll.ca.issuer;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

/**
 * Astrazione del servizio di Certificate Authority per l'emissione dei certificati Edge e IoT.
 *
 * @author Antonio Berti
 * @version 1.0
 * @since 31 March 2026
 */
public interface CertificateIssuer {
    IssuedCertificate issueDeviceCertificate(String deviceId, String csrPem, String sanUri) throws Exception;
}
