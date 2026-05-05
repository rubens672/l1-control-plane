package ai.berticloud.enroll.ca;

import ai.berticloud.shared.identity.DeviceIdentity;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import java.security.cert.X509Certificate;
import java.time.Instant;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

public interface CertificateIssuer {

    /** Bundle parse CSR: CSR + identity + urn */
    record ParsedCsr(PKCS10CertificationRequest csr, DeviceIdentity identity, String urn) {}

    /** Bundle cert firmato + metadati usati per DB persistence */
    record SignedCert(X509Certificate cert, String certSerialHex, Instant notAfter, String fingerprintSha256Hex) {}

    /**
     * Parse and validate the given CSR.
     */
    ParsedCsr parseAndValidateCsr(String csrPem, String expectedDeviceId) throws Exception;

    /**
     * Sign the CSR.
     */
    SignedCert sign(CaMaterial ca, ParsedCsr parsed) throws Exception;
}
