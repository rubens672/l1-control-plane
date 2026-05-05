package ai.berticloud.ingest.auth;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.StringReader;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

/*
 * Copyright (c) 2026 Berti AI & Cloud Architecture. All rights reserved.
 */

@Service
public class RootCaPublicKeyProvider {
    private final String certPem;
    private PublicKey rootCaPublicKey;

    public RootCaPublicKeyProvider(@Value("${app.ca.issuingCertSecret}") String certPem) {
        this.certPem = certPem;
    }

    @PostConstruct
    public void init() {
        X509Certificate cert = parseCert(certPem);
        this.rootCaPublicKey = cert.getPublicKey();
    }

    public PublicKey getRootCaPublicKey() {
        return rootCaPublicKey;
    }

    private static X509Certificate parseCert(String pem) {
        try (PEMParser p = new PEMParser(new StringReader(pem))) {
            Object o = p.readObject();
            if (!(o instanceof X509CertificateHolder h))
                throw new IllegalArgumentException("Invalid cert PEM");
            return new JcaX509CertificateConverter().getCertificate(h);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse Root CA certificate", e);
        }
    }
}
