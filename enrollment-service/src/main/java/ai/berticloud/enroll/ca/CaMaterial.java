package ai.berticloud.enroll.ca;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public record CaMaterial(
    PrivateKey issuingKey,
    X509Certificate issuingCert,
    String chainPem
) {}